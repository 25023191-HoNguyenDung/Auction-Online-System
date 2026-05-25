package com.auction.server.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auction.common.exception.AuctionConnectException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.server.dao.AutoBidProfileDao;
import com.auction.server.model.AutoBidProfile;
import com.auction.server.model.BidTransaction;
import com.auction.server.model.Bidder;


@ExtendWith(MockitoExtension.class)
public class AutoBidServiceTest {
    //Các Dependency giả lập (Mocks) để chặn tương tác với DB thật
    @Mock
    private AutoBidProfileDao autoBidProfileDao;
    @Mock
    private AuctionService auctionService;
    //Đối tượng thực tế dùng test
    private AutoBidService autoBidService;
    //Hằng số dữ liệu mẫu dùng chung 
    private static final long USER_ID = 1L;
    private static final long AUCTION_ID = 2L;

    //Thiết lập môi trường kiểm thử
    @BeforeEach
    void setUp() {
        //Lắp service thật vào các bộ phận giả lập
        autoBidService = new AutoBidService(autoBidProfileDao, auctionService);
    }

    //Tạo 1 profile mẫu dùng trong các test case
    private AutoBidProfile buildProfile(long id, long userId, BigDecimal maxBid, BigDecimal increment) {
        AutoBidProfile p = new AutoBidProfile();
        p.setId(id);
        p.setUser_id(userId);
        p.setAuction_id(AUCTION_ID);
        p.setMax_bid(maxBid);
        p.setIncrement(increment);
        p.setCreated_at(LocalDateTime.now());
        return p;
    }
    //Tạo bidder mẫu có tài khoản 10tr đô
    private BidTransaction buildBid(long userId, BigDecimal amount) {
        Bidder bidder = new Bidder("@testuser", userId, "test@gmail.com", "password", "BIDDER", new BigDecimal("10000000"), new ArrayList<>());
        return new BidTransaction(AUCTION_ID, bidder, amount);
    }


    //registerAutoBid - Kiểm thử đăng kí đấu giá tự động
    @Test
    @DisplayName("Test registerAutoBid - user chưa cài auto-bid bao giờ, lưu profile mới vào DB")
    void registerAutoBid_newProfile() throws AuctionConnectException {
        //Arrange: giả lập chưa có profile và ép DB lưu thành công
        BigDecimal maxBid = new BigDecimal("5000000");
        BigDecimal increment = new BigDecimal("100000");
        when(autoBidProfileDao.findByUserIdAndAuctionId(USER_ID, AUCTION_ID)).thenReturn(Optional.empty());
        AutoBidProfile saved = buildProfile(1L, USER_ID, maxBid, increment);
        when(autoBidProfileDao.save(any())).thenReturn(saved);

        //Act: gọi hàm đnawg kí auto-bid
        AutoBidProfile result = autoBidService.registerAutoBid(USER_ID, AUCTION_ID, maxBid, increment);

        //Assert: Kiểm tra - đối chiếu kết quả và kiểm tra các hàm DAO được gọi
        assertNotNull(result);
        assertEquals(0, maxBid.compareTo(result.getMax_bid()), "The final bid must match the input data.");
        verify(autoBidProfileDao).save(any());
        verify(autoBidProfileDao, never()).update(any());
    }

    @Test
    @DisplayName("Test registerAutoBid - user đã cài auto-bid, cập nhật thông tin vào profile cũ")
    void registerAutoBid_updateProfile() throws AuctionConnectException {
        //Arrange: giả lập DB đã có profile của user
        BigDecimal oldMax = new BigDecimal("3000000");
        BigDecimal newMax = new BigDecimal("5000000");
        BigDecimal incr = new BigDecimal("200000");
        AutoBidProfile existing = buildProfile(1L, USER_ID, oldMax, incr);
        when(autoBidProfileDao.findByUserIdAndAuctionId(USER_ID, AUCTION_ID)).thenReturn(Optional.of(existing));
        AutoBidProfile updated = buildProfile(1L, USER_ID, newMax, incr);
        when(autoBidProfileDao.update(any())).thenReturn(updated);

        //Act: gọi hàm đăng kí mức giá mới 
        AutoBidProfile result = autoBidService.registerAutoBid(USER_ID, AUCTION_ID, newMax, incr);

        //Assert: đảm bảo chỉ gọi lệnh cập nhật thay vì lưu mới
        assertEquals(0, newMax.compareTo(result.getMax_bid()), "The final bid must be updated to the new value.");
        verify(autoBidProfileDao).update(any());
        verify(autoBidProfileDao,never()).save(any());
    }

    @Test
    @DisplayName("Test registerAutoBid - nhập số tiền không đúng quy định")
    void registerAutoBid_invalidBid() {
        //Arrange: giả lập tham số bước giá tăng hợp lệ
        BigDecimal validInc = new BigDecimal("100000");

        //Act: Đóng gói các hành động đặt giá bị lỗi
        Executable bidNull = () -> autoBidService.registerAutoBid(USER_ID, AUCTION_ID, null, validInc);  //không đặt giá
        Executable bidZero = () -> autoBidService.registerAutoBid(USER_ID, AUCTION_ID, BigDecimal.ZERO, validInc);   //đặt giá = 0
        Executable bidNegative = () -> autoBidService.registerAutoBid(USER_ID, AUCTION_ID, new BigDecimal("-1000"), validInc);  //đặt giá âm

        //Assert: đảm bảo việc đặt giá trên đều ném lỗi
        assertThrows(IllegalArgumentException.class, bidNull);
        assertThrows(IllegalArgumentException.class, bidZero);
        assertThrows(IllegalArgumentException.class, bidNegative);
        verifyNoInteractions(autoBidProfileDao);    //đảm bảo không gọi xuóng DB
    }

    @Test
    @DisplayName("Test registerAutoBid - increment không hợp lệ")
    void registerAutoBid_invalidIncrement() {
        // Arrange: maxBid hợp lệ, chỉ thay đổi increment để kiểm tra validate
        BigDecimal validMax = new BigDecimal("5000000");
        
        //Act: đóng gói các hành động đặt bước giá bị lỗi
        Executable incNull = () -> autoBidService.registerAutoBid(USER_ID, AUCTION_ID, validMax, null);
        Executable incZero     = () -> autoBidService.registerAutoBid(USER_ID, AUCTION_ID, validMax, BigDecimal.ZERO);
        Executable incNegative = () -> autoBidService.registerAutoBid(USER_ID, AUCTION_ID, validMax, new BigDecimal("-1"));
        
        //Assert: đảm bảo các việc đặt giá trên đều lỗi
        assertThrows(IllegalArgumentException.class, incNull);
        assertThrows(IllegalArgumentException.class, incZero);
        assertThrows(IllegalArgumentException.class, incNegative);
        verifyNoInteractions(autoBidProfileDao);
    }
 

    //cancelAutoBid: Kiểm thử hủy auto-bid 
    @Test
    @DisplayName("Test cancelAutoBid - có cài auto-bid, xóa auto-bid thành công -> trả về true")
    void cancelAutoBid_success() {
        //Arrangee: giả lập DB có profile đang chạy và lệnh xóa thành công
        AutoBidProfile profile = buildProfile(1L, USER_ID, new BigDecimal("5000000"), new BigDecimal("100000"));
        when(autoBidProfileDao.findByUserIdAndAuctionId(USER_ID, AUCTION_ID)).thenReturn(Optional.of(profile));
        when(autoBidProfileDao.deleteById(1L)).thenReturn(true);

        //Act: gọi hàm boolean hủy auto-bid
        boolean result = autoBidService.cancelAutoBid(USER_ID, AUCTION_ID);

        //Assert: đảm bảo trả về true và DAO nhận được lệnh xóa đúng ID
        assertTrue(result, "Successful cancellation must return true.");
        verify(autoBidProfileDao).deleteById(1L);
    }

    @Test
    @DisplayName("Test cancelAutoBid - chưa cài auto-bid -> không có gì để xóa, trả về false")
    void cancelAutoBid_profileNotFound() {
        //Arrange: giả lập DB không tìm thấy profile nào
        when(autoBidProfileDao.findByUserIdAndAuctionId(USER_ID, AUCTION_ID)).thenReturn(Optional.empty());

        //Act: gọi hàm boolean hủy auto-bid
        boolean result = autoBidService.cancelAutoBid(USER_ID, AUCTION_ID);

        //Assert: đảm bảo trả về false
        assertFalse(result, "If there is nothing to delete, it must return false.");
        verify(autoBidProfileDao, never()).deleteById(anyLong());
    }


    //getAutoBidProfile - lấy thông tin hiện tại, trả về đúng profile
    @Test
    @DisplayName("Test getAutoBidProfile") 
    void getAutoBidProfile_exists_returnProfile() {
        //Arrange: giả lập có sẵn profile
        AutoBidProfile profile = buildProfile(1L, USER_ID, new BigDecimal("5000000"), new BigDecimal("100000"));
        when(autoBidProfileDao.findByUserIdAndAuctionId(USER_ID, AUCTION_ID)).thenReturn(Optional.of(profile));

        //Act: gọi hàm lấy thông tin
        Optional<AutoBidProfile> result = autoBidService.getAutoBidProfile(USER_ID, AUCTION_ID);

        //Assert: kiểm tra thông tin trùng khớp
        assertTrue(result.isPresent());
        assertEquals(USER_ID, result.get().getUser_id());
    }


    //placeBidAutomatically - Kiểm thử hàm đặt giá tự động
    @Test
    @DisplayName("Test placeBidAutomatically - đặt giá tự động thành công, trả về true")
    void placeBidAutomatically_success() throws Exception {
        //Arrange: giả lập hàm đặt giá nội bộ(placeBidInternal) hoạt động bình thường và trả về giao dịch hợp lệ
        BigDecimal amount = new BigDecimal("3000000");
        when(auctionService.placeBidInternal(AUCTION_ID, USER_ID, amount)).thenReturn(buildBid(USER_ID, amount));

        //Act: gọi hàm boolean tự động đặt giá
        boolean result = autoBidService.placeBidAutomatically(AUCTION_ID, USER_ID, amount);

        //Assert: đảm bảo kết quả trả về là true
        assertTrue(result, "Price set successfully. Returning true.");
    }

    @Test
    @DisplayName("Test placeBidAutomatically - lỗi đặt giá do xung đột thời gian thực, trả về false")
    void placeBidAutomatically_invalidBid() throws Exception {
        //Arrange: giả lập tình huổng race condition: lúc tính toán bid thì hợp lệ, nhưng khi gửi lệnh đi thì đã có người đặt giá cao hơn trước
        BigDecimal amount = new BigDecimal("500000");
        doThrow(new InvalidBidException("Bid too low")).when(auctionService).placeBidInternal(AUCTION_ID, USER_ID, amount);

        //Act: thực hiện gọi hàm boolean tự động đặt giá
        boolean result = autoBidService.placeBidAutomatically(AUCTION_ID, USER_ID, amount);

        //Assert: đảm bảo hệ thống bắt lỗi, trả về false, tránh sập 
        assertFalse(result, "Price setting missed. Skipping and returning false.");
    }

    @Test
    @DisplayName("Test placeBidAutomatically - lỗi thời gian phiên đấu giá ")
    void placeBidAutomatically_runtime() throws Exception {
        //Arrange: giả lập phiên đấu giá đã đóng
        BigDecimal amount = new BigDecimal("3000000");
        doThrow(new AuctionTimeException("Auction not running")).when(auctionService).placeBidInternal(AUCTION_ID, USER_ID, amount);

        //Act: Đóng gói hành động Executable để kiểm tra ngoại lệ
        Executable action = () -> autoBidService.placeBidAutomatically(AUCTION_ID, USER_ID, amount);

        //Assert: đảm bảo hệ thống ném RuntimeException để dừng tiến trình auto-bid
        assertThrows(RuntimeException.class, action, "Auction has already ended.");
    }


    //processAutoBids - kiểm thử tiến trình tự động đặt giá
    @Test
    @DisplayName("Test processAutoBids - không có hồ sơ auto-bid")
    void processAutoBids_noProfiles() {
        //Arrange: giả lập không có người dùng cài đặt tự động đặt giá
        when(autoBidProfileDao.findByAuctionId(AUCTION_ID)).thenReturn(Collections.emptyList());

        //Act: gọi hàm thực hiện auto-bid
        autoBidService.processAutoBids(AUCTION_ID, new BigDecimal("1000000"), 99L);

        //Assert: đảm bảo hệ thống không gọi đến service đặt giá
        verifyNoInteractions(auctionService);
    }

    @Test
    @DisplayName("Test processAutoBids - người dùng đang dẫn đầu -> bỏ qua, tránh tự nâng giá")
    void processAutoBids_skipHighestBidder() {
        //Arrange: giả lập DB có 1 hồ sơ auto-bid của USER_ID
        AutoBidProfile profile = buildProfile(1L, USER_ID, new BigDecimal("8000000"), new BigDecimal("100000"));
        when(autoBidProfileDao.findByAuctionId(AUCTION_ID)).thenReturn(List.of(profile));

        //Act: gọi hàm chạy tiến trình với thông tin người đặt giá cao nhất
        autoBidService.processAutoBids(AUCTION_ID, new BigDecimal("2000000"), USER_ID);

        //Assert: đảm bảo hệ thống không tạo lệnh đặt giá mới cho người dẫn đầu
        verifyNoInteractions(auctionService);
    }

    @Test 
    @DisplayName("Test processAutoBids - người dùng mất vị trí dẫn đầu -> tự động đặt giá mới")
    void processAutoBids_autoPlaceNextBid() throws Exception {
        //Arrange: người dùng có ID 999L đang dẫn đầu với giá 1tr đô
        long otherBidder = 999L; 
        BigDecimal current = new BigDecimal("1000000"); 
        BigDecimal increment = new BigDecimal("200000"); // bước giá
        BigDecimal nextBid = current.add(increment);    //giá tiếp theo là 1200000
        AutoBidProfile profile = buildProfile(1L, USER_ID, new BigDecimal("5000000"), increment);
        when(autoBidProfileDao.findByAuctionId(AUCTION_ID)).thenReturn(List.of(profile));
        when(auctionService.placeBidInternal(AUCTION_ID, USER_ID, nextBid)).thenReturn(buildBid(USER_ID, nextBid));

        //Act: gọi hàm kích hoạt tiến trình xử lí auto-bid
        autoBidService.processAutoBids(AUCTION_ID, current, otherBidder);

        //Assert: đảm bảo hệ thống gọi lệnh đặt giá chính xác \
        verify(auctionService, times(1)).placeBidInternal(AUCTION_ID, USER_ID, nextBid);
    }

    @Test
    @DisplayName("processAutoBids - Xử lý luân phiên giữa 2 hồ sơ auto-bid cho đến khi 1 hồ sơ chạm Max Bid")
    void processAutoBids_twoUsers_bidAlternatelyUntilMaxReached() throws Exception {
        //Arrange: Thiết lập 2 hồ sơ đấu giá tự động cạnh tranh nhau.
        long user1 = 1L; // User 1 có mức giá tối đa là 2.000.000
        long user2 = 2L; // User 2 có mức giá tối đa là 3.000.000
        BigDecimal current = new BigDecimal("1000000"); 
        BigDecimal increment = new BigDecimal("500000"); 

        AutoBidProfile p1 = buildProfile(1L, user1, new BigDecimal("2000000"), increment);
        AutoBidProfile p2 = buildProfile(2L, user2, new BigDecimal("3000000"), increment);

        when(autoBidProfileDao.findByAuctionId(AUCTION_ID)).thenReturn(List.of(p1, p2));

        // Giả lập hệ thống phê duyệt mọi lệnh đặt giá hợp lệ
        when(auctionService.placeBidInternal(anyLong(), anyLong(), any(BigDecimal.class)))
                .thenAnswer(invocation -> {
                    long uid = invocation.getArgument(1);
                    BigDecimal amount = invocation.getArgument(2);
                    return buildBid(uid, amount);
                });

        //Act: gọi hàm kích hoạt tiến trình quét với giá hiện tại là 1.000.000.
        autoBidService.processAutoBids(AUCTION_ID, current, 999L);

        //Assertt: Diễn biến đặt giá giữa 2 người dùng.
        // Lượt 1: User 1 giành quyền dẫn đầu với giá 1tr5 đô
        verify(auctionService).placeBidInternal(AUCTION_ID, user1, new BigDecimal("1500000"));
        // Lượt 2: User 2 trả cao hơn và dẫn đầu với giá 2tr đô
        verify(auctionService).placeBidInternal(AUCTION_ID, user2, new BigDecimal("2000000"));
        // Lượt 3: Đáng lẽ lên mức 2tr5, nhưng User 1 chỉ cài Max Bid là 2tr -> Quá trình cạnh tranh kết thúc.
        verify(auctionService, never()).placeBidInternal(AUCTION_ID, user1, new BigDecimal("2500000"));
    }

    @Test
    @DisplayName("processAutoBids - Ngăn chặn vòng lặp vô hạn (Infinite Loop) bằng giới hạn số lượt (50 lượt)")
    void processAutoBids_stopLoop() throws Exception {
        //Arrange: Thiết lập 2 hồ sơ auto-bid với ngân sách tối đa rất lớn để tạo kịch bản cạnh tranh dài hạn.
        long user1 = 1L;
        long user2 = 2L;
        BigDecimal current = new BigDecimal("1000000");
        BigDecimal increment = new BigDecimal("10000"); 
        BigDecimal hugeMax = new BigDecimal("999999999999"); // Giới hạn giá cực lớn

        AutoBidProfile p1 = buildProfile(1L, user1, hugeMax, increment);
        AutoBidProfile p2 = buildProfile(2L, user2, hugeMax, increment);

        when(autoBidProfileDao.findByAuctionId(AUCTION_ID)).thenReturn(List.of(p1, p2));
        when(auctionService.placeBidInternal(anyLong(), anyLong(), any(BigDecimal.class)))
                .thenAnswer(invocation -> {
                long uid = invocation.getArgument(1);
                BigDecimal amount = invocation.getArgument(2);
                return buildBid(uid, amount);
                });

        //Act: gọi hàm kích hoạt tiến trình auto-bid
        autoBidService.processAutoBids(AUCTION_ID, current, 999L);

        //Assert: Đảm bảo tiến trình bị ngắt đúng ở lượt giao dịch thứ 50 để tránh treo ứng dụng (Deadlock/Infinite Loop), mặc dù chưa chạm Max Bid.
        verify(auctionService, times(50)).placeBidInternal(eq(AUCTION_ID), anyLong(), any(BigDecimal.class));
    }
}