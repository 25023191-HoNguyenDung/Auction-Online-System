package com.auction.server.network;

import com.auction.server.observer.AuctionEventPublisher;
import com.auction.server.observer.ClientAuctionObserver;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
// quản lí observer( tạo đkí observer vào ClientAuctionObserver và hủy khi client rời)
public class SubscriptionRegistry {
    private static SubscriptionRegistry instance;
    private final AuctionEventPublisher publisher = AuctionEventPublisher.getInstance();
    // client đag theo dõi các phiên đgia nào
    private final ConcurrentHashMap<String, Set<Long>> clientSubscriptions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ClientAuctionObserver> observerMap = new ConcurrentHashMap<>();

    private SubscriptionRegistry() {
    }
    public static synchronized SubscriptionRegistry getInstance(){
        if(instance==null) instance = new SubscriptionRegistry();
        return instance;
    }

    //đki client theo dõi phiên đgia theo tg thực
    public void subscribe(String clientId, long auctionId, PrintWriter out){
        String key = buildKey(clientId, auctionId);
        // tránh subscribe trùng
        if(observerMap.containsKey(key)) return;
        // tạo observer cho client này
        ClientAuctionObserver observer = new ClientAuctionObserver(clientId,out);

        observerMap.put(key,observer);
        clientSubscriptions.computeIfAbsent(clientId, id -> ConcurrentHashMap.newKeySet()).add(auctionId);
        publisher.subscribe(auctionId,observer); // lưu observer này vào phiên đgia
        System.out.println("Client " + clientId + " đã subscribe phiên " + auctionId + ", tổng observer: " + publisher.getObserverCount(auctionId) + ")");
    }

    // client ngừng theo dõi 1 phiên
    public void unsubscribe(String clientId,long auctionId){
        String key = buildKey(clientId,auctionId);
        ClientAuctionObserver observer = observerMap.remove(key);
        if(observer !=null){ //ktra observer có tồn tại không
            publisher.unsubscribe(auctionId,observer);
            Set<Long> auctions = clientSubscriptions.get(clientId);
            if(auctions!=null){
                auctions.remove(auctionId);
                if (auctions.isEmpty()) clientSubscriptions.remove(clientId);
            }
        }
        System.out.println("Client " + clientId
                + " đã unsubscribe phiên " + auctionId);
    }

    //hủy tất cả theo dõi của 1 phiên
    public void unsubscribeAll(String clientId) {
        Set<Long> auctions = clientSubscriptions.remove(clientId);
        if (auctions == null) return;

        for (long auctionId : auctions) {
            String key = buildKey(clientId, auctionId);
            ClientAuctionObserver observer = observerMap.remove(key);
            if (observer != null) {
                publisher.unsubscribe(auctionId, observer);
            }
        }

        System.out.println("Client " + clientId
                + " đã unsubscribe tất cả phiên (" + auctions.size() + " phiên).");
    }

    //ds phiên client đang theo dõi
    public Set<Long> getSubscribedAuctions(String clientId) {
        return clientSubscriptions.getOrDefault(clientId, Collections.emptySet());
    }

    //ktra client có đang theo dõi phiên không
    public boolean isSubscribed(String clientId, long auctionId) {
        return observerMap.containsKey(buildKey(clientId, auctionId));
    }
    // tạo key duy nhất cho cặp
    private String buildKey(String clientId, long auctionId) {
        return clientId + ":" + auctionId;
    }
}
