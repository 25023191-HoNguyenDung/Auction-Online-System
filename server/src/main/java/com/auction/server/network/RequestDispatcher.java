package com.auction.server.network;

import java.io.PrintWriter;
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
import com.auction.server.model.User;
import com.auction.server.service.AuctionServiceImpl;
import com.auction.server.dao.ItemDao;
import com.auction.server.dao.jdbc.JdbcItemDao;
import com.auction.server.model.Item;

// điều hướng request đến server phù hợp
public class RequestDispatcher {
    private final AuctionServiceImpl auctionService;
    private final UserDao userDao;
    private final ProtocolMapper mapper;
    private final ItemDao itemDao; // THÊM MỚI 
    private final Object writeLock = new Object();

    public RequestDispatcher() {
        this.auctionService = new AuctionServiceImpl();
        this.userDao = new JdbcUserDao();
        this.mapper = new ProtocolMapper();
        this.itemDao = new JdbcItemDao(); // THÊM MỚI

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
                bidStrings.add(bidderName + "  →  $" + String.format("%,.0f", b.getBidAmount().doubleValue()));
            }
            // Sắp xếp giao dịch mới nhất lên đầu
            java.util.Collections.reverse(bidStrings);
            
            return new AuctionSummaryItem(
                a.getId(), 
                name, 
                desc, 
                cat, 
                a.getCurrent_price(), 
                a.getStatus().name(), 
                a.getEnd_time().toInstant(ZoneOffset.UTC),
                bidStrings
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


}