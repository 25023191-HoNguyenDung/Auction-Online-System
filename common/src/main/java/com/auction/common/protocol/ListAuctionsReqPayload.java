package com.auction.common.protocol;
// gửi yêu cầu lên server để lấy danh sách phiên đấu giá
public class ListAuctionsReqPayload {
    private final String userId; // id người gửi yêu cầu
    private final Integer page;
    private final Integer size;
    private final String statusFilter;

    public ListAuctionsReqPayload(String userId, Integer page, Integer size, String statusFilter) {
        this.userId = userId;
        this.page = page; //
        this.size = size; // số phiên đấu giá trong 1 page
        this.statusFilter = statusFilter; // trạng thái
    }

    public String getUserId() {
        return userId;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }

    public String getStatusFilter() {
        return statusFilter;
    }
}