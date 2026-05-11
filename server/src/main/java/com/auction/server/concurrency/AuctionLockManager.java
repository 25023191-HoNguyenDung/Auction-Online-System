package com.auction.server.concurrency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionLockManager {
    private static AuctionLockManager instance;
    //  ConcurrentHashMap:nhiều thread truy cập vào dữ liệu chung 1 cách an toàn
    private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>(); // ReentrantLock -> chỉ 1 thread xử lý 1 auction tại 1 thời điểm

    public AuctionLockManager() {
    }
    //singleton
    public static synchronized AuctionLockManager getInstance(){
        if(instance==null) instance = new AuctionLockManager();
        return instance;
    }
    // lấy khóa nếu chưa có thì tạo
    public ReentrantLock getLock(long auctionId){
        return lockMap.computeIfAbsent(auctionId,id->new ReentrantLock(true));
    }
    // khóa phiên đgia
    public void lock(long auctionId){
        getLock(auctionId).lock();
    }
    //mở khóa
    public void unlock(long auctionId){
        ReentrantLock lock = lockMap.get(auctionId);
        if(lock==null && lock.isHeldByCurrentThread()){
            lock.unlock();
        }
    }
    // dọn lock sau khi kết thúc phiên
    public void removeLock(long auctionId){
        lockMap.remove(auctionId);
    }



}
