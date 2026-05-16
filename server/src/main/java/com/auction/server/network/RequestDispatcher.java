package com.auction.server.network;

import com.auction.common.exception.AuctionConnectException;
import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.protocol.*;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.jdbc.JdbcUserDao;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.User;
import com.auction.server.observer.AuctionEventPublisher;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AuctionServiceImpl;
import com.auction.server.service.AutoBidService;

import java.io.PrintWriter;
import java.time.ZoneOffset;
import java.util.Optional;

// điều hướng request đến server phù hợp
public class RequestDispatcher {
    private final AuctionService auctionService;
    private final AutoBidService autoBidService;
    private final UserDao userDao;
    private final ProtocolMapper mapper;
    private final SubscriptionRegistry subscriptionRegistry; // qlý các client đag theo dõi auction
    private final AuctionEventPublisher publisher; // gửi event khi có thay đổi

    public RequestDispatcher() {
        this.auctionService = new AuctionServiceImpl();
        this.autoBidService = new AutoBidService(auctionService);
        this.userDao = new JdbcUserDao();
        this.mapper = new ProtocolMapper();
        this.subscriptionRegistry = SubscriptionRegistry.getInstance();
        this.publisher = AuctionEventPublisher.getInstance();
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
                case LIST_AUCTIONS_REQ: {
                    handleListAuctions(envelope, correlationId, out);
                    break;
                }
                case PLACE_BID_REQ: {
                    handlePlaceBid(envelope, correlationId, out);
                    break;
                }
                default: {
                    sendError(out, correlationId, ErrorCode.UNSUPPORTED_PROTOCOL, "MessageType không được hỗ trợ: " + envelope.getType());
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
            sendError(out,correlationId,ErrorCode.AUTH_INVALID_CREDENTIALS,"sai username hoặc password");
            return;
        }
        User user = userOpt.get();
        LoginResPayload res = new LoginResPayload(true, user.get_ID(), user.get_user_name(), user.getRole()); // tạo payload phản hồi
        send(out, mapper.buildResponse(MessageType.LOGIN_RES,correlationId,res)); // gửi dưới dạng JSON
    }

    // lấy ds phiên đgia gửi client
    private void handleListAuctions(MessageEnvelope envelope, String correlationId, PrintWriter out) throws AuctionConnectException {
        ListAuctionsReqPayload req = mapper.parsePayload(envelope, ListAuctionsReqPayload.class); // lấy dữ liệu từ req -> obj
        // lấy ds auctions( trống : lấy tất cả, ko thì lấy các auction có trạng thái)
        var auctions = (req.getStatusFilter() == null || req.getStatusFilter().isBlank()) ? auctionService.getAllAuctions() : auctionService.getAuctionsByStatus(AuctionStatus.valueOf(req.getStatusFilter()));
        // chuyển auction-> AuctionSummaryItem(chỉ chứa tt hữu ích cho client)
        var summaries = auctions.stream().map(a -> new AuctionSummaryItem(a.getId(), String.valueOf(a.getItem_id()), a.getCurrent_price(), a.getStatus().name(), a.getEnd_time().toInstant(ZoneOffset.UTC))).toList();
        // gửi res về client
        send(out, mapper.buildResponse(MessageType.LIST_AUCTIONS_RES, correlationId, new ListAuctionsResPayload(summaries, summaries.size())));
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
    }

    // Message->JSON r gửi
    private void send(PrintWriter out, MessageEnvelope envelope) {
        synchronized (out) {
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
