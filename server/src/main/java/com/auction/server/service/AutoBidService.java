package com.auction.server.service;

import com.auction.server.dao.AutoBidProfileDao;
import com.auction.server.dao.BidDao;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.jdbc.JdbcAutoBidProfileDao;
import com.auction.server.dao.jdbc.JdbcBidDao;
import com.auction.server.dao.jdbc.JdbcUserDao;

public class AutoBidService {
    private final AutoBidProfileDao autoBidProfileDao;
    private final BidDao bidDao;
    private final UserDao userDao;
    private final AuctionService auctionService;

    public AutoBidService(AutoBidProfileDao autoBidProfileDao,
                          BidDao bidDao,
                          UserDao userDao,
                          AuctionService auctionService) {
        this.autoBidProfileDao = autoBidProfileDao;
        this.bidDao            = bidDao;
        this.userDao           = userDao;
        this.auctionService    = auctionService;
    }

    public AutoBidService(AuctionService auctionService) {
        this(new JdbcAutoBidProfileDao(), new JdbcBidDao(), new JdbcUserDao(), auctionService);
    }
}
