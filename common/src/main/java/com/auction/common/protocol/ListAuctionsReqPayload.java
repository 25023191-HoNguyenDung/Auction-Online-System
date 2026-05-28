package com.auction.common.protocol;

// gửi yêu cầu lên server để lấy danh sách phiên đấu giá
public class ListAuctionsReqPayload {
    private long userId; // id người gửi yêu cầu
    private int page;
    private int size;
    private String statusFilter; // trạng thái


    public ListAuctionsReqPayload(long userId, int page, int size, String statusFilter) {
        this.userId = userId;
        this.page = page; //
        this.size = size; // số phiên đấu giá trong 1 page
        this.statusFilter = statusFilter; // trạng thái
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }
}