package com.auction.server.network;

import java.io.PrintWriter;
import java.util.Optional;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;

import com.auction.common.exception.AuctionConnectException;
import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.protocol.*;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.jdbc.JdbcUserDao;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.User;
import com.auction.server.service.AuctionServiceImpl;
import com.auction.server.dao.ItemDao;
import com.auction.server.dao.jdbc.JdbcItemDao;
import com.auction.server.model.Item;
import com.auction.server.model.Auction;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.jdbc.JdbcAuctionDao;
import com.auction.server.config.DatabaseConfig;

import com.auction.server.service.AutoBidService;
import com.auction.server.model.AutoBidProfile;

// dispatch requests to the appropriate handlers
public class RequestDispatcher {
    private final AuctionServiceImpl auctionService;
    private final UserDao userDao;
    private final ProtocolMapper mapper;
    private final ItemDao itemDao; // NEW ADDITION
    private final AuctionDao auctionDao; // NEW ADDITION
    private final AutoBidService autoBidService;
    private final Object writeLock = new Object();

    public RequestDispatcher() {
        this.auctionService = new AuctionServiceImpl();
        this.userDao = new JdbcUserDao();
        this.mapper = new ProtocolMapper();
        this.itemDao = new JdbcItemDao(); // NEW ADDITION
        this.auctionDao = new JdbcAuctionDao(); // NEW ADDITION
        this.autoBidService = new AutoBidService(this.auctionService);
    }


    // nhận req từ client và chuyển đến hàm xử lý tương ứng
    public void dispatch(MessageEnvelope envelope, String clientId, PrintWriter out) {
        String correlationId = envelope.getMessageId(); // để biết đag sử lý req nào
        try {
            switch (envelope.getType()) {
                case LOGIN_REQ:{
                    handleLogin(envelope, correlationId, out);
                    break;
                }
                case REGISTER_REQ: {
                    handleRegister(envelope, correlationId, out);
                    break;
                }
                case LIST_AUCTIONS_REQ: {
                    handleListAuctions(envelope, correlationId, out);
                    break;
                }
                case PLACE_BID_REQ: {
                    handlePlaceBid(envelope, correlationId, out);
                    break;
                }
                case REGISTER_AUTOBID_REQ: {
                    handleRegisterAutoBid(envelope, correlationId, out);
                    break;
                }
                case CANCEL_AUTOBID_REQ: {
                    handleCancelAutoBid(envelope, correlationId, out);
                    break;
                }
                case GET_AUTOBID_REQ: {
                    handleGetAutoBid(envelope, correlationId, out);
                    break;
                }
                case SUBSCRIBE_REQ: {
                    long auctionId = mapper.parsePayload(envelope, SubscriptionReqPayload.class).getAuctionId();
                    SubscriptionRegistry.getInstance().subscribe(clientId, auctionId, out);
                    break;
                }
                case UNSUBSCRIBE_REQ: {
                    long auctionId = mapper.parsePayload(envelope, SubscriptionReqPayload.class).getAuctionId();
                    SubscriptionRegistry.getInstance().unsubscribe(clientId, auctionId);
                    break;
                }
                case DEPOSIT_REQ: {                  // từ main: xử lý nạp tiền
                    handleDeposit(envelope, correlationId, out);
                    break;
                }
                case SUBMIT_LISTING_REQ: {
                    handleSubmitListing(envelope, correlationId, out);
                    break;
                }
                case ADMIN_ACTION_REQ: {
                    handleAdminAction(envelope, correlationId, out);
                    break;
                }
                case LIST_USERS_REQ: {
                    handleListUsers(envelope, correlationId, out);
                    break;
                }
                case UPDATE_USER_REQ: {
                    handleUpdateUser(envelope, correlationId, out);
                    break;
                }
                default: {
                    sendError(out, correlationId, ErrorCode.UNSUPPORTED_PROTOCOL, "MessageType is not supported: " + envelope.getType());
                }
            }
        } catch (Exception e) {
            sendError(out, correlationId, ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    // check username và password
    private void handleLogin(MessageEnvelope envelope, String correlationId, PrintWriter out){
        LoginReqPayload req = mapper.parsePayload(envelope,LoginReqPayload.class); // lấy dữ liệu từ payload -> obj LoginReqPayload
        Optional<User> userOpt = userDao.findByUsername(req.getUsername()); // tìm username trong db
        if(userOpt.isEmpty() || !userOpt.get().check_password(req.getPassword())){ // nếu ko thấy username hoặc sai password
            sendError(out,correlationId,ErrorCode.AUTH_INVALID_CREDENTIALS,"Invalid username or password");
            return;
        }
        User user = userOpt.get();

        // Lấy số dư tài khoản thực tế từ database
        java.math.BigDecimal balance = java.math.BigDecimal.ZERO;
        if (user instanceof com.auction.server.model.Bidder) {
            balance = ((com.auction.server.model.Bidder) user).getAccount_balance();
        } else if (user instanceof com.auction.server.model.Seller) {
            balance = ((com.auction.server.model.Seller) user).getAccount_balance();
        }
        if (balance == null) {
            balance = java.math.BigDecimal.ZERO;
        }

        LoginResPayload res = new LoginResPayload(true, user.get_ID(), user.get_user_name(), user.getRole(), balance); // tạo payload phản hồi
        send(out, mapper.buildResponse(MessageType.LOGIN_RES,correlationId,res)); // gửi dưới dạng JSON
    }

    private void handleRegister(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        RegisterReqPayload req = mapper.parsePayload(envelope, RegisterReqPayload.class);

        // Dùng lại AuthController đã có
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("username", req.getUsername());
        payload.put("email",    req.getEmail());
        payload.put("password", req.getPassword());
        payload.put("role",     req.getRole());

        com.auction.server.controller.AuthController authController =
            new com.auction.server.controller.AuthController(userDao instanceof com.auction.server.dao.jdbc.JdbcUserDao
                ? new com.auction.server.service.AuthService((com.auction.server.dao.UserDao) userDao)
                : new com.auction.server.service.AuthService(new com.auction.server.dao.jdbc.JdbcUserDao()));

        java.util.Map<String, Object> result = authController.handleRegister(payload);

        boolean success = Boolean.TRUE.equals(result.get("success"));
        if (success) {
            LoginResPayload res = new LoginResPayload(true, 0, req.getUsername(), req.getRole());
            send(out, mapper.buildResponse(MessageType.REGISTER_RES, correlationId, res));
        } else {
            sendError(out, correlationId, ErrorCode.INVALID_MESSAGE, (String) result.get("message"));
        }
    }

    // lấy ds phiên đgia gửi client
    private void handleListAuctions(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        ListAuctionsReqPayload req = mapper.parsePayload(envelope, ListAuctionsReqPayload.class);
        
        var auctions = (req.getStatusFilter() == null || req.getStatusFilter().isBlank()) 
            ? auctionService.getAllAuctions() 
            : auctionService.getAuctionsByStatus(AuctionStatus.valueOf(req.getStatusFilter()));
            
        // Map sang DTO mới chứa thông tin sản phẩm thật
        var summaries = auctions.stream().map(a -> {
            // Lấy thông tin sản phẩm từ Database
            Item item = itemDao.findById(a.getItem_id()).orElse(null);
            String name = (item != null) ? item.getItemName() : "Unknown Item";
            String desc = (item != null) ? item.getDescription() : "No description";
            String cat  = (item != null) ? item.getCategory() : "Art";
            
             // Lấy lịch sử đặt giá thực tế từ Database
             List<com.auction.server.model.BidTransaction> bids = auctionService.getBidHistory(a.getId());
             List<String> bidStrings = new java.util.ArrayList<>();
             for (com.auction.server.model.BidTransaction b : bids) {
                 String bidderName = b.getBidder() != null && b.getBidder().get_user_name() != null && !b.getBidder().get_user_name().isEmpty() 
                     ? b.getBidder().get_user_name() : "bidder" + b.getBidderId();
                 bidStrings.add(bidderName + "  →  $" + String.format("%,.0f", b.getBidAmount().doubleValue()) + "  →  " + b.getTimeBidding().toString());
             }
            // Sắp xếp giao dịch mới nhất lên đầu
            java.util.Collections.reverse(bidStrings);
            
            String sellerName = "Unknown";
            long sellerId = a.getSeller_id();
            Optional<User> sellerOpt = userDao.findById(sellerId);
            if (sellerOpt.isPresent()) {
                sellerName = sellerOpt.get().get_user_name();
            }

            return new AuctionSummaryItem(
                a.getId(), 
                name, 
                desc, 
                cat, 
                a.getCurrent_price(), 
                a.getStatus().name(), 
                a.getEnd_time().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                bidStrings,
                sellerId,
                sellerName,
                a.getStart_time() != null ? a.getStart_time().atZone(java.time.ZoneId.systemDefault()).toInstant() : null,
                a.getStarting_price()
            );
        }).toList();
        
        send(out, mapper.buildResponse(MessageType.LIST_AUCTIONS_RES, correlationId, 
             new ListAuctionsResPayload(summaries, summaries.size())));
    }

    private void handlePlaceBid(MessageEnvelope envelope, String correlationId, PrintWriter out) throws InvalidBidException, AuctionConnectException, AuctionMisMatchException, AuctionTimeException {
        PlaceBidReqPayload req = mapper.parsePayload(envelope, PlaceBidReqPayload.class); // đọc req
        // thực hiện đặt giá
        var bid = auctionService.placeBid(req.getAuctionId(), req.getBidderId(), req.getAmount());
        // tạo res đặt giá thành công
        PlaceBidResPayload res = new PlaceBidResPayload(
                true,
                req.getAuctionId(),
                bid.getBidAmount(),
                bid.getBidderId()
        );
        send(out, mapper.buildResponse(MessageType.PLACE_BID_RES, correlationId, res)); // gửi kq về client
        // publisher.publish() và autoBidService.processAutoBids() đã được gọi trong AuctionServiceImpl.placeBid()
    }

    private void handleRegisterAutoBid(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        RegisterAutoBidReqPayload req = mapper.parsePayload(envelope, RegisterAutoBidReqPayload.class);
        try {
            autoBidService.registerAutoBid(req.getUserId(), req.getAuctionId(), req.getMaxBid(), req.getIncrement());
            
            // Kích hoạt tiến trình AutoBid lập tức sau khi đăng ký thành công
            Auction auction = auctionService.getAuctionById(req.getAuctionId());
            autoBidService.processAutoBids(req.getAuctionId(), auction.getCurrent_price(), auction.getWinner_bidder_id());

            RegisterAutoBidResPayload res = new RegisterAutoBidResPayload(true, "AutoBid registered successfully!");
            send(out, mapper.buildResponse(MessageType.REGISTER_AUTOBID_RES, correlationId, res));
        } catch (Exception e) {
            send(out, mapper.buildResponse(MessageType.REGISTER_AUTOBID_RES, correlationId, new RegisterAutoBidResPayload(false, e.getMessage())));
        }
    }

    private void handleCancelAutoBid(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        CancelAutoBidReqPayload req = mapper.parsePayload(envelope, CancelAutoBidReqPayload.class);
        try {
            boolean success = autoBidService.cancelAutoBid(req.getUserId(), req.getAuctionId());
            if (success) {
                CancelAutoBidResPayload res = new CancelAutoBidResPayload(true, "AutoBid canceled successfully!");
                send(out, mapper.buildResponse(MessageType.CANCEL_AUTOBID_RES, correlationId, res));
            } else {
                CancelAutoBidResPayload res = new CancelAutoBidResPayload(false, "No active AutoBid profile found to cancel.");
                send(out, mapper.buildResponse(MessageType.CANCEL_AUTOBID_RES, correlationId, res));
            }
        } catch (Exception e) {
            send(out, mapper.buildResponse(MessageType.CANCEL_AUTOBID_RES, correlationId, new CancelAutoBidResPayload(false, e.getMessage())));
        }
    }

    private void handleGetAutoBid(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        GetAutoBidReqPayload req = mapper.parsePayload(envelope, GetAutoBidReqPayload.class);
        try {
            Optional<AutoBidProfile> profileOpt = autoBidService.getAutoBidProfile(req.getUserId(), req.getAuctionId());
            if (profileOpt.isPresent()) {
                AutoBidProfile profile = profileOpt.get();
                Auction auction = auctionService.getAuctionById(req.getAuctionId());
                java.math.BigDecimal nextBid = auction.getCurrent_price().add(profile.getIncrement());
                boolean isExpired = (auction.getCurrent_price().compareTo(profile.getMax_bid()) > 0)
                        || (profile.getUser_id() != auction.getWinner_bidder_id() && nextBid.compareTo(profile.getMax_bid()) > 0);
                if (isExpired) {
                    autoBidService.cancelAutoBid(profile.getUser_id(), profile.getAuction_id());
                    GetAutoBidResPayload res = new GetAutoBidResPayload(false, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO);
                    send(out, mapper.buildResponse(MessageType.GET_AUTOBID_RES, correlationId, res));
                } else {
                    GetAutoBidResPayload res = new GetAutoBidResPayload(true, profile.getMax_bid(), profile.getIncrement());
                    send(out, mapper.buildResponse(MessageType.GET_AUTOBID_RES, correlationId, res));
                }
            } else {
                GetAutoBidResPayload res = new GetAutoBidResPayload(false, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO);
                send(out, mapper.buildResponse(MessageType.GET_AUTOBID_RES, correlationId, res));
            }
        } catch (Exception e) {
            sendError(out, correlationId, ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    // Message->JSON r gửi
    private void send(PrintWriter out, MessageEnvelope envelope) {
        synchronized (writeLock) {
            out.println(mapper.toJson(envelope));
            out.flush();
        }
    }

    // tạo res lỗi r gửi
    private void sendError(PrintWriter out, String correlationId,
                           ErrorCode code, String message) {
        send(out, mapper.buildErrorResponse(correlationId, code, message));
    }

    // Xử lý nạp tiền — cập nhật account_balance thật vào DB (từ main)
    private void handleDeposit(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        DepositReqPayload req = mapper.parsePayload(envelope, DepositReqPayload.class);

        if (req.getAmount() == null || req.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            sendError(out, correlationId, ErrorCode.INVALID_MESSAGE, "Số tiền nạp phải lớn hơn 0");
            return;
        }

        User user = userDao.findById(req.getUserId()).orElse(null);
        if (user == null) {
            sendError(out, correlationId, ErrorCode.INVALID_MESSAGE, "Không tìm thấy user id: " + req.getUserId());
            return;
        }

        java.math.BigDecimal newBalance;
        if (user instanceof com.auction.server.model.Bidder bidder) {
            newBalance = (bidder.getAccount_balance() == null ? java.math.BigDecimal.ZERO : bidder.getAccount_balance())
                         .add(req.getAmount());
            bidder.setAccount_balance(newBalance);
        } else if (user instanceof com.auction.server.model.Seller seller) {
            newBalance = (seller.getAccount_balance() == null ? java.math.BigDecimal.ZERO : seller.getAccount_balance())
                         .add(req.getAmount());
            seller.setAccount_balance(newBalance);
        } else {
            sendError(out, correlationId, ErrorCode.INVALID_MESSAGE, "Admin không thể nạp tiền");
            return;
        }

        userDao.update(user);
        System.out.println("[Deposit] User " + req.getUserId() + " deposited " + req.getAmount() + " → new balance: " + newBalance);

        DepositResPayload res = new DepositResPayload(true, newBalance, "Nạp tiền thành công");
        send(out, mapper.buildResponse(MessageType.DEPOSIT_RES, correlationId, res));
    }

    private void handleSubmitListing(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        SubmitListingReqPayload req = mapper.parsePayload(envelope, SubmitListingReqPayload.class);
        try {
            Item item = new Item();
            item.setSellerId(req.getSellerId());
            item.setItemName(req.getItemName());
            item.setDescription(req.getDescription());
            item.setCategory(req.getCategory().toUpperCase());
            item.setStartingPrice(java.math.BigDecimal.valueOf(req.getStartingPrice()));
            item.setCurrentPrice(java.math.BigDecimal.valueOf(req.getStartingPrice()));
            item = itemDao.save(item);

            Auction auction = new Auction();
            auction.setItem_id(item.getItemId());
            auction.setSeller_id(req.getSellerId());
            auction.setStarting_price(java.math.BigDecimal.valueOf(req.getStartingPrice()));
            auction.setCurrent_price(java.math.BigDecimal.valueOf(req.getStartingPrice()));
            auction.setStatus(AuctionStatus.OPEN);
            auction.setStart_time(java.time.LocalDateTime.now());
            auction.setEnd_time(java.time.LocalDateTime.now().plusMinutes(req.getDurationMinutes()));
            auction = auctionDao.save(auction);

            SubmitListingResPayload res = new SubmitListingResPayload(true, "Listing submitted successfully!", auction.getId());
            send(out, mapper.buildResponse(MessageType.SUBMIT_LISTING_RES, correlationId, res));
        } catch (Exception e) {
            send(out, mapper.buildResponse(MessageType.SUBMIT_LISTING_RES, correlationId, new SubmitListingResPayload(false, e.getMessage(), 0L)));
        }
    }

    private void handleAdminAction(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        AdminActionReqPayload req = mapper.parsePayload(envelope, AdminActionReqPayload.class);
        try {
            String action = req.getAction().toUpperCase();
            long auctionId = req.getAuctionId();

            if ("APPROVE".equals(action)) {
                Auction auction = auctionService.getAuctionById(auctionId);
                java.time.Duration originalDuration = java.time.Duration.between(auction.getStart_time(), auction.getEnd_time());
                auction.setStatus(AuctionStatus.RUNNING);
                auction.setStart_time(java.time.LocalDateTime.now());
                auction.setEnd_time(java.time.LocalDateTime.now().plus(originalDuration));
                auctionDao.update(auction);
            } else if ("REJECT".equals(action) || "REMOVE".equals(action)) {
                auctionDao.deleteById(auctionId);
            } else if ("END".equals(action)) {
                auctionService.closeAuction(auctionId);
            } else {
                throw new IllegalArgumentException("Unknown admin action: " + action);
            }

            AdminActionResPayload res = new AdminActionResPayload(true, "Action " + action + " executed successfully!");
            send(out, mapper.buildResponse(MessageType.ADMIN_ACTION_RES, correlationId, res));
        } catch (Exception e) {
            send(out, mapper.buildResponse(MessageType.ADMIN_ACTION_RES, correlationId, new AdminActionResPayload(false, e.getMessage())));
        }
    }

    private void handleListUsers(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        try {
            List<User> dbUsers = userDao.findAll();
            List<UserSummaryItem> summaries = new java.util.ArrayList<>();
            for (User u : dbUsers) {
                double balance = 0.0;
                if (u instanceof com.auction.server.model.Bidder b && b.getAccount_balance() != null) {
                    balance = b.getAccount_balance().doubleValue();
                } else if (u instanceof com.auction.server.model.Seller s && s.getAccount_balance() != null) {
                    balance = s.getAccount_balance().doubleValue();
                }
                summaries.add(new UserSummaryItem(u.get_ID(), u.get_user_name(), u.get_email(), u.getRole(), balance));
            }
            ListUsersResPayload res = new ListUsersResPayload(summaries);
            send(out, mapper.buildResponse(MessageType.LIST_USERS_RES, correlationId, res));
        } catch (Exception e) {
            sendError(out, correlationId, ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private void handleUpdateUser(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        UpdateUserReqPayload req = mapper.parsePayload(envelope, UpdateUserReqPayload.class);
        try {
            String sql = "UPDATE users SET role = ? WHERE id = ?";
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, req.getNewRole().toUpperCase());
                ps.setLong(2, req.getUserId());
                ps.executeUpdate();
            }
            UpdateUserResPayload res = new UpdateUserResPayload(true, "User role updated successfully!");
            send(out, mapper.buildResponse(MessageType.UPDATE_USER_RES, correlationId, res));
        } catch (Exception e) {
            send(out, mapper.buildResponse(MessageType.UPDATE_USER_RES, correlationId, new UpdateUserResPayload(false, e.getMessage())));
        }
    }
}