package com.auction.server.service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.jdbc.JdbcAuctionDao;
import com.auction.server.model.Auction;

// Scheduler tự động quét và chuyển trạng thái các phiên đấu giá theo thời gian
public class AuctionClosingService {
    private static final int CHECK_INTERVAL_SECONDS = 10; 
    private final AuctionDao auctionDao;
    private final AuctionService auctionService;
    private final ScheduledExecutorService scheduler; //Bộ lập lịch để tự động quét mở/đóng phiên theo chu kỳ

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

    //khời động scheduler
    public void start(){
        scheduler.scheduleAtFixedRate(() -> {
            processReadyToOpenAuctions();   //quét và mở các phiên đến giờ
            processExpiredAuctions();       //quét và đóng các phiên hết giờ
    }, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS); // chạy method 10s 1 lần
        System.out.println("Đã khởi động, kiểm tra mỗi " + CHECK_INTERVAL_SECONDS + "giây");
    }

    // tắt scheduler
    public void stop(){
        scheduler.shutdown(); // ngăn không cho scheduler nhận task mới
        try{
            if(!scheduler.awaitTermination(5, TimeUnit.SECONDS)){ // cho phép các task đang chạy dở có tối đa 5s để hoàn thành nốt
                scheduler.shutdownNow();    // nếu 5s mà chưa xong -> ép buộc dừng 
            }
        }catch (InterruptedException e){
            scheduler.shutdownNow();            //quá trình chờ tắt bị gián đoạn -> ép buộc dừng
            Thread.currentThread().interrupt();
        }
        System.out.println("[AuctionScheduler] Đã dừng.");
    }

    //Logic tự động mở phiên
    private void processReadyToOpenAuctions() {
        try {
            //Tìm các phiên ở trạng thái OPEN và thời gian start_time
            List<Auction> opening = auctionDao.findOpenReadyToStart();
            //không có phiên nào cần mở -> thoát
            if (opening.isEmpty()) return;
            System.out.println("[AuctionScheduler] Tìm thấy " + opening.size() + " phiên đến giờ cần mở.");
            for (Auction auction : opening) {
                try {
                    auctionService.openAuction(auction.getId());
                    System.out.println("[AuctionScheduler] Đã mở phiên id = " + auction.getId());
                } catch (Exception e) {
                    System.err.println("[AuctionScheduler] Lỗi mở phiên id = " + auction.getId() + ": " + e.getMessage());
                }
            } 
        }
        catch (Exception e) {
            System.err.println("[AuctonSchehduler] Lỗi kiểm tra phiên cần mở: " + e.getMessage());
        }
    }

    //  Logic tự động đóng phiên 
    private void processExpiredAuctions() {
        try {
            //Tìm các phiên trạng thái RUNNING và thời gian end_time
            List<Auction> expired = auctionDao.findExpiredRunning();
            //Không có phiên nào đang chạy -> thoát
            if (expired.isEmpty()) return;
            System.out.println("[AuctionScheduler] Tìm thấy " + expired.size() + " phiên hết giờ cần đóng.");
            for (Auction auction : expired) {
                try {
                    auctionService.closeAuction(auction.getId());
                    System.out.println("[AuctionScheduler] Đã đóng phiên id =" + auction.getId());
                } catch (Exception e) {
                    System.err.println("[AuctionScheduler] Lỗi đóng phiên id =" + auction.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[AuctionScheduler] Lỗi kiểm tra phiên cần đóng: " + e.getMessage());
        }
    }
}
