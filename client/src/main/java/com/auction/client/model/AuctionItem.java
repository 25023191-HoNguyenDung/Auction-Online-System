package com.auction.client.model;

public class AuctionItem {

    private int    id;
    private String title;
    private String subtitle;      // mô tả ngắn / edition
    private String category;      // Art, Electronics, Vehicles, Watches, Jewellery
    private String status;        // LIVE, ENDING_SOON, ENDED, PENDING
    private double startPrice;    // giá khởi điểm
    private double currentBid;    // giá hiện tại
    private int    totalBids;     // số lượt đặt giá
    private long   endsAtEpoch;   // thời gian kết thúc (Unix timestamp, giây)
    private String imageUrl;      // URL ảnh từ server (null nếu chưa có)
    private int    sellerId;      // id của người bán
    private String sellerName;    // tên người bán

    public AuctionItem() {}

    public AuctionItem(int id, String title, String subtitle,
                       String category, String status,
                       double startPrice, double currentBid,
                       int totalBids, long endsAtEpoch,
                       String imageUrl, int sellerId, String sellerName) {
        this.id          = id;
        this.title       = title;
        this.subtitle    = subtitle;
        this.category    = category;
        this.status      = status;
        this.startPrice  = startPrice;
        this.currentBid  = currentBid;
        this.totalBids   = totalBids;
        this.endsAtEpoch = endsAtEpoch;
        this.imageUrl    = imageUrl;
        this.sellerId    = sellerId;
        this.sellerName  = sellerName;
    }

    // ── Getters ──────────────────────────────────────────────
    public int    getId()          { return id; }
    public String getTitle()       { return title; }
    public String getSubtitle()    { return subtitle; }
    public String getCategory()    { return category; }
    public String getStatus()      { return status; }
    public double getStartPrice()  { return startPrice; }
    public double getCurrentBid()  { return currentBid; }
    public int    getTotalBids()   { return totalBids; }
    public long   getEndsAtEpoch() { return endsAtEpoch; }
    public String getImageUrl()    { return imageUrl; }
    public int    getSellerId()    { return sellerId; }
    public String getSellerName()  { return sellerName; }

    // ── Setters ──────────────────────────────────────────────
    public void setId(int id)                   { this.id = id; }
    public void setTitle(String title)          { this.title = title; }
    public void setSubtitle(String subtitle)    { this.subtitle = subtitle; }
    public void setCategory(String category)    { this.category = category; }
    public void setStatus(String status)        { this.status = status; }
    public void setStartPrice(double v)         { this.startPrice = v; }
    public void setCurrentBid(double v)         { this.currentBid = v; }
    public void setTotalBids(int v)             { this.totalBids = v; }
    public void setEndsAtEpoch(long v)          { this.endsAtEpoch = v; }
    public void setImageUrl(String imageUrl)    { this.imageUrl = imageUrl; }
    public void setSellerId(int sellerId)       { this.sellerId = sellerId; }
    public void setSellerName(String name)      { this.sellerName = name; }

    // ── Helper ───────────────────────────────────────────────
    /** Số giây còn lại tính từ bây giờ */
    public int secondsLeft() {
        long now = System.currentTimeMillis() / 1000;
        return (int) Math.max(0, endsAtEpoch - now);
    }

    public boolean isLive()        { return "LIVE".equals(status); }
    public boolean isEndingSoon()  { return "ENDING_SOON".equals(status); }
    public boolean isEnded()       { return "ENDED".equals(status); }
}
