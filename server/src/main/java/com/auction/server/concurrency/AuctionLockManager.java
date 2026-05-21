package com.auction.server.concurrency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionLockManager {
    private static AuctionLockManager instance;
    //  ConcurrentHashMap:nhiều thread truy cập vào dữ liệu chung 1 cách an toàn
    private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>(); // ReentrantLock -> chỉ 1 thread xử lý 1 auction tại 1 thời điểm

    private AuctionLockManager() {} //bắt buộc dùng getInstance(), không tạo thẳng, tránh race condition

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

    //thử giành quyền kiểm soát (lock) trong thời gian timeout 
    //true nếu lấy được lock, false nếu hết timeout
    public boolean tryLock(long auctionId, long timeout, TimeUnit unit) throws InterruptedException {
        return getLock(auctionId).tryLock(timeout, unit);
    }

    //mở khóa (chỉ unlock nếu thread hiện tại đang giữ lock)
    public void unlock(long auctionId){
        ReentrantLock lock = lockMap.get(auctionId);
        if(lock!=null && lock.isHeldByCurrentThread()){
            lock.unlock();
        }
    }

    // dọn lock sau khi kết thúc phiên
    public void removeLock(long auctionId){
        lockMap.computeIfPresent(auctionId, (id, lock) -> {
        //nếu vẫn còn thread đang giữ lock -> không xóa
        //nếu khóa không bị luồng nào giữ: 
            if (!lock.isLocked()) return null;  //trả về null (xóa khỏi map)
            return lock;
        });
    }

    //số lượng lock đang được quản lý
    public int activeLockCount() {
        return lockMap.size();
    }

}
