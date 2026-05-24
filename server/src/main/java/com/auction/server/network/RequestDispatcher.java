package com.auction.server.network;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;

import com.auction.common.exception.AuctionConnectException;
import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.protocol.*;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.jdbc.JdbcUserDao;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Bidder;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.service.AuctionServiceImpl;
import com.auction.server.dao.ItemDao;
import com.auction.server.dao.jdbc.JdbcItemDao;
import com.auction.server.model.Item;

/**
 * PHIÊN BẢN ĐÃ SỬA:
 * - Thêm case DEPOSIT_REQ → handleDeposit()
 * - handleDeposit() cập nhật account_balance thật vào DB
 *
 * PATH: server/src/main/java/com/auction/server/network/RequestDispatcher.java
 */
public class RequestDispatcher {
    private final AuctionServiceImpl auctionService;
    private final UserDao userDao;
    private final ProtocolMapper mapper;
    private final ItemDao itemDao;
    private final Object writeLock = new Object();

    public RequestDispatcher() {
        this.auctionService = new AuctionServiceImpl();
        this.userDao = new JdbcUserDao();
        this.mapper = new ProtocolMapper();
        this.itemDao = new JdbcItemDao();
    }

    public void dispatch(MessageEnvelope envelope, String clientId, PrintWriter out) {
        String correlationId = envelope.getMessageId();
        try {
            switch (envelope.getType()) {
                case LOGIN_REQ: {
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
                case DEPOSIT_REQ: {                  // ← THÊM MỚI
                    handleDeposit(envelope, correlationId, out);
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
                default: {
                    sendError(out, correlationId, ErrorCode.UNSUPPORTED_PROTOCOL,
                            "MessageType is not supported: " + envelope.getType());
                }
            }
        } catch (Exception e) {
            sendError(out, correlationId, ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private void handleLogin(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        LoginReqPayload req = mapper.parsePayload(envelope, LoginReqPayload.class);
        Optional<User> userOpt = userDao.findByUsername(req.getUsername());
        if (userOpt.isEmpty() || !userOpt.get().check_password(req.getPassword())) {
            sendError(out, correlationId, ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid username or password");
            return;
        }
        User user = userOpt.get();

        BigDecimal balance = BigDecimal.ZERO;
        if (user instanceof Bidder) {
            balance = ((Bidder) user).getAccount_balance();
        } else if (user instanceof Seller) {
            balance = ((Seller) user).getAccount_balance();
        }
        if (balance == null) balance = BigDecimal.ZERO;

        LoginResPayload res = new LoginResPayload(true, user.get_ID(), user.get_user_name(), user.getRole(), balance);
        send(out, mapper.buildResponse(MessageType.LOGIN_RES, correlationId, res));
    }

    private void handleRegister(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        RegisterReqPayload req = mapper.parsePayload(envelope, RegisterReqPayload.class);

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("username", req.getUsername());
        payload.put("email",    req.getEmail());
        payload.put("password", req.getPassword());
        payload.put("role",     req.getRole());

        com.auction.server.controller.AuthController authController =
            new com.auction.server.controller.AuthController(
                userDao instanceof JdbcUserDao
                    ? new com.auction.server.service.AuthService((UserDao) userDao)
                    : new com.auction.server.service.AuthService(new JdbcUserDao()));

        java.util.Map<String, Object> result = authController.handleRegister(payload);
        boolean success = Boolean.TRUE.equals(result.get("success"));
        if (success) {
            LoginResPayload res = new LoginResPayload(true, 0, req.getUsername(), req.getRole());
            send(out, mapper.buildResponse(MessageType.REGISTER_RES, correlationId, res));
        } else {
            sendError(out, correlationId, ErrorCode.INVALID_MESSAGE, (String) result.get("message"));
        }
    }

    private void handleListAuctions(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        ListAuctionsReqPayload req = mapper.parsePayload(envelope, ListAuctionsReqPayload.class);

        var auctions = (req.getStatusFilter() == null || req.getStatusFilter().isBlank())
            ? auctionService.getAllAuctions()
            : auctionService.getAuctionsByStatus(AuctionStatus.valueOf(req.getStatusFilter()));

        var summaries = auctions.stream().map(a -> {
            Item item = itemDao.findById(a.getItem_id()).orElse(null);
            String name = (item != null) ? item.getItemName()    : "Unknown Item";
            String desc = (item != null) ? item.getDescription() : "No description";
            String cat  = (item != null) ? item.getCategory()    : "Art";

            List<com.auction.server.model.BidTransaction> bids = auctionService.getBidHistory(a.getId());
            List<String> bidStrings = new java.util.ArrayList<>();
            for (com.auction.server.model.BidTransaction b : bids) {
                String bidderName = b.getBidder() != null && b.getBidder().get_user_name() != null
                    ? b.getBidder().get_user_name() : "bidder" + b.getBidderId();
                bidStrings.add(bidderName + "  →  $" + String.format("%,.0f", b.getBidAmount().doubleValue()));
            }
            java.util.Collections.reverse(bidStrings);

            return new AuctionSummaryItem(
                a.getId(), name, desc, cat,
                a.getCurrent_price(), a.getStatus().name(),
                a.getEnd_time().toInstant(ZoneOffset.UTC),
                bidStrings
            );
        }).toList();

        send(out, mapper.buildResponse(MessageType.LIST_AUCTIONS_RES, correlationId,
             new ListAuctionsResPayload(summaries, summaries.size())));
    }

    private void handlePlaceBid(MessageEnvelope envelope, String correlationId, PrintWriter out)
            throws InvalidBidException, AuctionConnectException, AuctionMisMatchException, AuctionTimeException {
        PlaceBidReqPayload req = mapper.parsePayload(envelope, PlaceBidReqPayload.class);
        var bid = auctionService.placeBid(req.getAuctionId(), req.getBidderId(), req.getAmount());
        PlaceBidResPayload res = new PlaceBidResPayload(
                true, req.getAuctionId(), bid.getBidAmount(), bid.getBidderId());
        send(out, mapper.buildResponse(MessageType.PLACE_BID_RES, correlationId, res));
    }

    /**
     * ← THÊM MỚI: Xử lý nạp tiền — cập nhật account_balance thật vào DB.
     */
    private void handleDeposit(MessageEnvelope envelope, String correlationId, PrintWriter out) {
        DepositReqPayload req = mapper.parsePayload(envelope, DepositReqPayload.class);

        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            sendError(out, correlationId, ErrorCode.INVALID_MESSAGE, "Số tiền nạp phải lớn hơn 0");
            return;
        }

        // Lấy user từ DB
        User user = userDao.findById(req.getUserId())
                .orElse(null);
        if (user == null) {
            sendError(out, correlationId, ErrorCode.INVALID_MESSAGE, "Không tìm thấy user id: " + req.getUserId());
            return;
        }

        // Cộng tiền vào số dư
        BigDecimal newBalance;
        if (user instanceof Bidder bidder) {
            newBalance = (bidder.getAccount_balance() == null ? BigDecimal.ZERO : bidder.getAccount_balance())
                         .add(req.getAmount());
            bidder.setAccount_balance(newBalance);
        } else if (user instanceof Seller seller) {
            newBalance = (seller.getAccount_balance() == null ? BigDecimal.ZERO : seller.getAccount_balance())
                         .add(req.getAmount());
            seller.setAccount_balance(newBalance);
        } else {
            sendError(out, correlationId, ErrorCode.INVALID_MESSAGE, "Admin không thể nạp tiền");
            return;
        }

        // Lưu vào DB
        userDao.update(user);
        System.out.println("[Deposit] User " + req.getUserId() + " deposited " + req.getAmount() + " → new balance: " + newBalance);

        DepositResPayload res = new DepositResPayload(true, newBalance, "Nạp tiền thành công");
        send(out, mapper.buildResponse(MessageType.DEPOSIT_RES, correlationId, res));
    }

    private void send(PrintWriter out, MessageEnvelope envelope) {
        synchronized (writeLock) {
            out.println(mapper.toJson(envelope));
            out.flush();
        }
    }

    private void sendError(PrintWriter out, String correlationId, ErrorCode code, String message) {
        send(out, mapper.buildErrorResponse(correlationId, code, message));
    }
}