package com.auction.server.observer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// quản lý danh sách observer theo từng phiên đấu giá
public class AuctionEventPublisher {
    private static AuctionEventPublisher instance;
    //lưu danh sách các Observer theo từng phiên đgia
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<AuctionObserver>> observerMap = new ConcurrentHashMap<>();
    public static synchronized AuctionEventPublisher getInstance(){
        if(instance==null) instance = new AuctionEventPublisher();
        return instance;
    }
    //đki observer theo dõi 1 phiên đgia, gọi khi client mở màn hình 1 phiên
    public void subscribe(long auctionId, AuctionObserver observer){
        observerMap.computeIfAbsent(auctionId, id -> new CopyOnWriteArrayList<>()).add(observer); // nếu chưa có ds observer cho auctionId thì tạo mới
    }
    // hủy đki gọi khi client đóng hoặc ngắt kết nối
    public void unsubscribe(long auctionId, AuctionObserver observer){
        List<AuctionObserver> observers = observerMap.get(auctionId); // lấy ds các observer theo dõi auction
        if (observers != null){
            observers.remove(observer);
            //dọn Map nếu ko còn ai theo dõi
            if(observers.isEmpty()){
                observerMap.remove(auctionId);
            }
        }
    }
    // hủy đki tất cả các phiên gọi khi client ngắt kết nối
    public void unsubscribeAll(AuctionObserver observer){
        observerMap.forEach((auctionId,observers) -> observers.remove(observer)); // xóa observer khỏi ds từng auction
    }
    //gửi event đến tất cả observer đang theo dõi phiên đấu giá
    public void publish(AuctionEvent event){
        List<AuctionObserver> observers = observerMap.get(event.getAuctionId());
        if(observers == null || observers.isEmpty()) return;
        for(AuctionObserver observer : observers){
            try{
                observer.onAuctionEvent(event);
            }catch (Exception e){
                // 1 observer lỗi ko ảnh hưởng đến những cái còn lại
                System.err.println("[AuctionEventPublisher] Lỗi notify observer: " + e.getMessage());
            }
        }
    }
    // số lượng observer đang theo dõi trong 1 phiên
    public int getObserverCount(long auctionId) {
        List<AuctionObserver> observers = observerMap.get(auctionId);
        return observers == null ? 0 : observers.size();
    }




}
