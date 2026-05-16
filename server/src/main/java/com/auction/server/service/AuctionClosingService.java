package com.auction.server.service;

import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.jdbc.JdbcAuctionDao;
import com.auction.server.model.Auction;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// tự động ktra/đóng các phiên đgia hết giờ
public class AuctionClosingService {
    private static final int check = 10; // ktra mỗi 10s
    private final AuctionDao auctionDao;
    private final AuctionService auctionService;
    private final ScheduledExecutorService scheduler; // chạy tự động theo tg

    public AuctionClosingService(AuctionDao auctionDao, AuctionService auctionService) {
        this.auctionDao = auctionDao;
        this.auctionService = auctionService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> { // scheduler có 1 thread duy nhất
            Thread t = new Thread(r, "auction_closinng_scheduler");
            t.setDaemon(true); // thread nền server dừng thì dừng theo
            return t;
        });
    }

    public AuctionClosingService(AuctionService auctionService) {
        this(new JdbcAuctionDao(), auctionService);
    }

    //khời động ktra tự động
    public void start(){
        scheduler.scheduleAtFixedRate(() -> {checkAndCloseExpiredAuctions();}, 0, 10, TimeUnit.SECONDS); // chạy method 10s 1 lần
        System.out.println("Đã khởi động, kiểm tra mỗi " + check + "giây");
    }

    // tắt scheduler
    public void stop(){
        scheduler.shutdown(); // ko nhận task mới nữa
        try{
            if(!scheduler.awaitTermination(5, TimeUnit.SECONDS)){ // chờ 5s để dừng htoan
                scheduler.shutdownNow();
            }
        }catch (InterruptedException e){
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Đã dừng.");
    }

    //ktra cái nào hết giờ thì đóng
    private void checkAndCloseExpiredAuctions() {
        try {
            List<Auction> expired = auctionDao.findExpiredRunning(); // list đag running nhưng hết hạn
            if (expired.isEmpty()) return;

            System.out.println("Tìm thấy " + expired.size() + " phiên hết giờ.");

            for (Auction auction : expired) {
                try {
                    auctionService.cancelAuction(auction.getId());
                    System.out.println("Đã đóng phiên id=" + auction.getId());
                } catch (Exception e) {
                    System.err.println("Lỗi đóng phiên id=" + auction.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi kiểm tra phiên: " + e.getMessage());
        }
    }
}
