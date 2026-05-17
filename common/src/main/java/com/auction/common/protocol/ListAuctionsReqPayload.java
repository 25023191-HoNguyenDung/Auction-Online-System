package com.auction.common.protocol;
// gửi yêu cầu lên server để lấy danh sách phiên đấu giá
public class ListAuctionsReqPayload {
    private final long userId; // id người gửi yêu cầu
    private final int page;
    private final int size;
    private final String statusFilter;

    public ListAuctionsReqPayload(long userId, int page, int size, String statusFilter) {
        this.userId = userId;
        this.page = page; //
        this.size = size; // số phiên đấu giá trong 1 page
        this.statusFilter = statusFilter; // trạng thái
    }

    public long getUserId() {
        return userId;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public String getStatusFilter() {
        return statusFilter;
    }
}