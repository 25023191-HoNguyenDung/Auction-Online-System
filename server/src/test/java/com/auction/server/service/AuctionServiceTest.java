package com.auction.server.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.concurrency.TransactionManager;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidDao;
import com.auction.server.dao.UserDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;
import com.auction.server.model.Bidder;
import com.auction.server.model.Seller;
import com.auction.server.observer.AuctionEventPublisher;

@ExtendWith(MockitoExtension.class)     //Tự động khởi tạo các trường đánh dấu Mocks, quản lý vòng đời và dọn dẹp sau mỗi test case

public class AuctionServiceTest {
    //Các Dependency giả lập (Mocks) chặn tương tác với DB thật
    @Mock private  AuctionDao auctionDao;
    @Mock private  BidDao bidDao;  
    @Mock private  UserDao userDao;
    @Mock private  AutoBidService autoBidService;
    @Mock private  AuctionEventPublisher publisher; 
    @Mock private  TransactionManager transManager;

    private MockedStatic<AuctionLockManager> lockManagerMockedStatic; //Mock tĩnh cho lớp quản lý lock
    private AuctionLockManager mockLockManager; //Mock cụ thể để kiểm soát hành vi lock trong test
    private AuctionServiceImpl service; //Đối tượng thực thể cần đem ra kiểm thử

    //Dữ liệu mẫu dùng chung cho kiểm thử
    private static final long AUCTION_ID = 1L;
    private static final long BIDDER_ID = 2L;
    private static final long SELLER_ID = 3L;
    private static final BigDecimal STARTING_PRICE = new BigDecimal("1000");

    //Thiết lập môi trường kiểm thử trước/sau mỗi test case
    @BeforeEach
    void setUp() {
        //Chặn hàm getInstance của AuctionLogicManager và bắt trả về đối tượng giả lập
        lockManagerMockedStatic = mockStatic(AuctionLockManager.class); 
        mockLockManager = mock(AuctionLockManager.class);
        lockManagerMockedStatic.when(() -> AuctionLockManager.getInstance()).thenReturn(mockLockManager);
        //Khởi tạo đối tượng service thật với các dependency giả lập
        service = new AuctionServiceImpl(auctionDao, bidDao, userDao, autoBidService, publisher, transManager);
    }

    @AfterEach
    void tearDown() {
        //Dọn dẹp mock tĩnh sau mỗi test case để tránh ảnh hưởng chéo giữa các test
        if (lockManagerMockedStatic != null) {
            lockManagerMockedStatic.close();
        }
    }


    //Tạo 1 thực thể Auction mẫu để dùng trong các test case
    private Auction buildAuction(AuctionStatus status) {
        Auction a = new Auction();
        a.setId(AUCTION_ID);
        a.setItem_id(1L);
        a.setSeller_id(SELLER_ID);
        a.setStarting_price(STARTING_PRICE);
        a.setCurrent_price(STARTING_PRICE);
        a.setStatus(status);
        a.setStart_time(LocalDateTime.now().minusMinutes(10));   //Bắt đầu trước hiện tại 10p để đảm bảo đã qua thời gian bắt đầu đấu giá
        a.setEnd_time(LocalDateTime.now().plusHours(2));        //Kết thúc sau hiện tại 2h 
        a.setWinner_bidder_id(0L);         //Chưa có người thắng cuộc
        return a;
    }

    //Tạo 1 thực thể Bidder mẫu có tài khoản 10tr
    private Bidder buildBidder() {
        return new Bidder("@testbidder", BIDDER_ID, "@test@gmail.com", "password", "BIDDER", new BigDecimal("10000000"), new ArrayList<>());
    }

    //Giả lập TransactionManager để thực thi trực tiếp lambda mà không cần quản lý transaction thật
    public void stubTransactionRunsLambda () throws Exception {
        doAnswer(inv -> {
            TransactionManager.TransactionWork work = inv.getArgument(0);
        try {
            work.excecute(null); //Thực thi trực tiếp công việc mà không cần quản lý transaction thật
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
        }
        return null;
        }).when (transManager).executeInTransaction(any());
    }

    //getAllAuctions - Kiểm thử chức năng
    @Test
    @DisplayName("Test getAllAuctions - DAO trả về 1 danh sách đấu giá, service trả về đúng danh sách đó")
    void getAllAuctions_returnsList() {
        //Arrange: Chuẩn bị dữ liệu giả lập và hành vi của DAO
        List<Auction> mockList = List.of(buildAuction(AuctionStatus.RUNNING));
        when(auctionDao.findAll()).thenReturn(mockList);
        
        //Act: Gọi hàm nghiệp vụ cần kiểm thử của service
        List<Auction> result = service.getAllAuctions();
        
        //Assert: Đảm bảo số lượng phần tử khớp và đúng dữ liệu đã giả lập. Đảm bảo tầng DAO được gọi đúng 1 lần
        assertEquals(1, result.size());
        verify(auctionDao, times(1)).findAll();
    }

    @Test
    @DisplayName("Test getAllAuctions - DAO trả về null, service trả về danh sách rỗng")
    void getAllAuctions_returnsEmptyList() {
        // Arrange: Giả lập không có dữ liệu đấu giá nào trong DB
        when(auctionDao.findAll()).thenReturn(Collections.emptyList());
        
        // Act & Assert: Kết quả trả về bắt buộc phải là một danh sách trống
        assertTrue(service.getAllAuctions().isEmpty());
    }

    //getAuctionsByStatus - Kiểm thử trạng thái
    @Test
    @DisplayName("Test getAuctionsByStatus - lọc đúng theo status RUNNING, trả về danh sách chỉ chứa đấu giá đang chạy")
    void getAuctionsByStatus_returnsFilteredList() {
        //Arrange: giả lập DAO trả về ds lọc theo trạng thái RUNNING
        when(auctionDao.findByStatus(AuctionStatus.RUNNING))
        .thenReturn(List.of(buildAuction(AuctionStatus.RUNNING)));
        
        //Act: Kích hoạt bộ lọc trạng thái của service
        List<Auction> result = service.getAuctionsByStatus(AuctionStatus.RUNNING);
        
        //Assert: Xác thực kết quả lọc chính xác
        assertEquals(1, result.size());
        assertEquals(AuctionStatus.RUNNING, result.get(0).getStatus());
        verify(auctionDao).findByStatus(AuctionStatus.RUNNING);
    }

    @Test
    @DisplayName("Test getAuctionsByStatus - không có phiên nào khớp thì trả về rỗng")
    void getAuctionsByStatus_returnsEmptyList() {
        //Arrange: Giả lập không tìm thấy phiên nào có trạng thái FINISHED
        when(auctionDao.findByStatus(AuctionStatus.FINISHED))
                .thenReturn(Collections.emptyList());
        
                //Act & Assert: Kết quả trả về phải là một danh sách trống
        assertTrue(service.getAuctionsByStatus(AuctionStatus.FINISHED).isEmpty());
    }


    //getAuctionById - Kiểm thử tìm kiếm theo ID
    @Test
    @DisplayName("Test getAuctionById - tìm thấy đấu giá theo ID, trả về đúng đối tượng")
    void getAuctionById_returnsAuction() {
        //Arrange: giả lập tìm thấy phiên đấu giá đúng với ID mẫu
        when(auctionDao.findById(AUCTION_ID))
                .thenReturn(Optional.of(buildAuction(AuctionStatus.RUNNING)));
        
                //Act: gọi hàm tìm kiếm theo id
        Auction result = service.getAuctionById(AUCTION_ID);
        
        //Assert: Dữ liệu trả về không được rỗng. ID phải khớp
        assertNotNull(result); 
        assertEquals(AUCTION_ID, result.getId());
    }

    @Test
    @DisplayName("Test getAuctionById - không tìm thấy đấu giá theo ID, ném RuntimeException")
    void getAuctionById_notFound() {
        //Arrange: giả lập không tìm thấy phiên đấu giá nào với ID mẫu
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.empty());
        
        //Act & Assert: Kiểm tra hệ thống có ném đúng ngoại lệ kèm thông báo lỗi trực quan không
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.getAuctionById(AUCTION_ID));
        assertTrue(exception.getMessage().contains("Auction not found"));
    }

    //createAuction - Kiểm thử tạo mới đấu giá
    @Test
    @DisplayName ("Test createAuction - tạo đấu giá mới, tự set trạng thái là OPEN, current_price = starting_price và lưu vào DB") 
    void createAuction_setOpenAndSave() throws Exception {
        //Arrange: Chuẩn bị dữ liệu thô đầu vào cho hàm tạo đấu giá, dữ liệu ban đầu để trống
        Auction input = buildAuction(null);
        
        //Act: Nếu service lưu dữ liệu, DB giả lập sẽ tự gán thêm id = 999
        when(auctionDao.save(any(Auction.class))).thenAnswer(inv -> {
            Auction saved = inv.getArgument(0);
            saved.setId(999L); //Giả lập DB tự gán ID khi lưu
            return saved;
        });
        
        //Assert: Kiểm tra xem có đảm bảo quy tắc nghiệm vụ không
        Auction result = service.createAuction(input);
        //Đảm bảo mặc định là OPEN
        assertEquals(AuctionStatus.OPEN, result.getStatus());
        assertEquals(0, STARTING_PRICE.compareTo(result.getCurrent_price()));
        verify(auctionDao).save(input);
    }

    
    //getBidHistory - Kiểm thử lịch sử đấu giá
    @Test
    @DisplayName("Test getBidHistory - có lịch sử đấu giá, trả về đúng danh sách")
    void getBidHistory_returnBidsForAuction() {
        //Arrange: Chuẩn bị dữ liệu giả lập cho lịch sử chứa 1 giao dịch đặt giá 5000 đô
        Bidder bidder = buildBidder();
        BidTransaction bid = new BidTransaction(AUCTION_ID, bidder, new BigDecimal("5000"));
        when(bidDao.findByAuctionId(AUCTION_ID)).thenReturn(List.of(bid));
        
        //Act: Gọi hàm lấy lịch sử đấu giá
        List<BidTransaction> result = service.getBidHistory(AUCTION_ID);
        
        //Assert: Đảm bảo dữ liệu trả về khớp với dữ liệu lịch sử giả lập
        assertEquals(1, result.size());
        assertEquals(0, new BigDecimal("5000").compareTo(result.get(0).getBidAmount()));
        verify(bidDao).findByAuctionId(AUCTION_ID);
    }

    @Test
    @DisplayName("Test getBidHistory - không có lịch sử đấu giá, trả về danh sách rỗng")
    void getBidHistory_noBids() {
        //Arrange: Giả lập không có giao dịch đấu giá nào trong lịch sử
        when(bidDao.findByAuctionId(AUCTION_ID)).thenReturn(Collections.emptyList());
       
        //Act & Assert: Kết quả trả về phải là một danh sách trống
        assertTrue(service.getBidHistory(AUCTION_ID).isEmpty());
    }
    

    //getHighestBid - Kiểm thử giá đấu cao nhất
    @Test
    @DisplayName("Test getHighestBid - trả về giá đấu cao nhất")
    void getHighestBid_returnsMaxBid() {
        ///Arrange: giả lập tìm thấy giao dịch đấu giá mức cao nhất là 10000 đô
        Bidder bidder = buildBidder();
        BidTransaction highest = new BidTransaction(AUCTION_ID, bidder, new BigDecimal("10000"));
        when(bidDao.findHighestBidByAuctionId(AUCTION_ID)).thenReturn(Optional.of(highest));
        
        //Act: 
        BidTransaction result = service.getHighestBid(AUCTION_ID);
        
        //Assert: Đảm bảo giá đấu cao nhất khớp với dữ liệu giả lập
        assertNotNull(result);
        assertEquals(0, new BigDecimal("10000").compareTo(result.getBidAmount()));
    }

    @Test
    @DisplayName("Test getHighestBid - không có bid nào, trả về null")
    void getHighestBid_noBids() {
        
        //Arrange: Giả lập không có giao dịch đấu giá nào cho phiên này
        when(bidDao.findHighestBidByAuctionId(AUCTION_ID)).thenReturn(Optional.empty());
        
        //Act & Assert: Kết quả trả về phải là null
        assertNull(service.getHighestBid(AUCTION_ID));
    }


    //placeBid - Kiểm thử đặt giá đấu 
    //placeBid - Trường hợp thành công
    @Test
    @DisplayName("Test placeBid - đặt giá đấu hợp lệ, cập nhật giá hiện tại và lưu giao dịch đấu giá")
    void placeBid_successAndSaved() throws Exception {
        //Arrange: Chuẩn bị dữ liệu giả lập cho phiên đấu giá đang chạy, người đặt giá có đủ tiền và giá đặt cao hơn giá hiện tại
        Auction running = buildAuction(AuctionStatus.RUNNING);
        Bidder bidder = buildBidder();
        BigDecimal newBid = new BigDecimal(2000); 
        
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(running));
        when(userDao.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));
        when(userDao.update(any())).thenReturn(bidder);
        when(bidDao.save(any())).thenAnswer(inv -> {
            BidTransaction b = inv.getArgument(0);
            b.setId(99L); //Giả lập DB lưu thành công giao dịch đặt giá id = 99
            return b;
        });
        stubTransactionRunsLambda(); //Kích hoạt giả lập TransactionManager
        
        //Act: Thực thi hành động đặt giá đấu
        BidTransaction result = service.placeBid(AUCTION_ID, BIDDER_ID, newBid);
        //Assert: 
        
        //1. Xác thực dữ liệu giao dịch đặt giá thành công
        assertNotNull(result);
        assertEquals(0, newBid.compareTo(result.getBidAmount()));
        assertEquals(BIDDER_ID, result.getBidderId());
        
        //2. Kiểm thử lock
        InOrder lockOrder = inOrder(mockLockManager);   //Kiểm tra thứ tự gọi hàm của LockManager
        //Bắt buộc lock trước, unlock sau 
        lockOrder.verify(mockLockManager).lock(AUCTION_ID); 
        lockOrder.verify(mockLockManager).unlock(AUCTION_ID); 
        
        //3. Đảm  bảo sự kiện đặt giá thành công được phát đi toàn hệ thống
        verify(publisher, times(1)).publish(any());

        //4. Đảm bảo auto-bid được kích hoạt 
        verify(autoBidService, times(1)).processAutoBids(AUCTION_ID, newBid, BIDDER_ID);
    }

    @Test
    @DisplayName("Test placeBid - bid đầu tiên bằng starting_price thì được chấp nhận")
    void placeBid_firstBidEqualsStartingPrice() throws Exception {
        //Arrange: dữ liệu giả lập cho phiên đấu đang chạy, chưa có ai đặt giá
        Auction running = buildAuction(AuctionStatus.RUNNING);
        running.setWinner_bidder_id(0L); //đảm bảo chưa co người thắng cuộc
        Bidder bidder = buildBidder();
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(running));
        when(userDao.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));
        when(userDao.update(any())).thenReturn(bidder);
        when(bidDao.save(any())).thenAnswer(inv -> {
            BidTransaction b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });
        stubTransactionRunsLambda();

        //Act: người đầu tiên đặt giá bằng giá khởi điểm 1000 đô
        BidTransaction result = service.placeBid(AUCTION_ID, BIDDER_ID, STARTING_PRICE);

        //Assert: đảm bảo giao dịch này được chấp nhận và lưu thành công
        assertNotNull(result);
        assertEquals(BIDDER_ID, result.getBidderId());

    }


    //placeBid - Trường hợp thất bại
    @Test
    @DisplayName("Test placeBid - user không tồn tại thì ném RuntimeException, publisher không được gọi")
    void placeBid_userNotFound() {
        //Arrange: giả lập không tìm thấy người dùng với ID mẫu
        when(userDao.findById(BIDDER_ID)).thenReturn(Optional.empty());

        //Act & Assert: chặn hành vi đang thực hiện và ném lỗi
        assertThrows(RuntimeException.class, () -> service.placeBid(AUCTION_ID, BIDDER_ID, new BigDecimal("2000")));
        //Đảm bảo sự kiện đặt giá không được phát đi vì đã có lỗi
        verify(publisher, never()).publish(any());
    }

    @Test
    @DisplayName("Test placeBid - giá thấp hơn hoặc bằng giá hiện tại thì bị từ chối")
    void placeBid_bidLowerOrEquals() throws Exception {
        //Arrange: Phiên đấu giá đang chạy với giá 1000 đô, người thắng là người đặt giá đầu tiên = giá khỏi điểm
        //Giả lập người đặt giá thứ 2 cố gắng đặt giá thấp hơn hoặc bằng giá hiện tại
        Auction running = buildAuction(AuctionStatus.RUNNING);
        running.setWinner_bidder_id(999L);
        Bidder bidder = buildBidder();
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(running));
        when(userDao.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));
        stubTransactionRunsLambda();

        //Act & Assert: Người sau cố tính đặt giá <= giá hiện tại -> bị đẩy ra
        assertThrows(Exception.class, () -> service.placeBid(AUCTION_ID, BIDDER_ID, STARTING_PRICE));
        verify(publisher, never()).publish(any());
    }

    @Test 
    @DisplayName("Test placeBid - phiên đã kết thúc thì không cho đặt giá, bắt buộc unlock")
    void placeBid_auctionFinished() throws Exception {
        //Arrange: giả lập phiên đấu giá đã kết thúc
        Auction finished = buildAuction(AuctionStatus.FINISHED);
        Bidder bidder = buildBidder();
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(finished));
        when(userDao.findById(BIDDER_ID)).thenReturn(Optional.of(bidder));
        stubTransactionRunsLambda();

        //Act & Assert 1:: Phiên đấu giá đã kết thúc -> không cho đặt giá
        assertThrows(Exception.class, () -> service.placeBid(AUCTION_ID, BIDDER_ID, new BigDecimal("2000")));
        //Assert 2: Đảm bảo lock được giải phóng dù có lỗi xảy ra
        verify(mockLockManager).unlock(AUCTION_ID);
        verify(publisher, never()).publish(any());
    }

    @Test
    @DisplayName("Test placeBid - user là Seller (không phải Bidder) thì không cho đặt giá")
    void placeBid_userIsSeller() throws Exception {
        //Arrange: giả lập phiên đấu giá đang chạy, người đặt giá là người bán
        Seller seller = new Seller("@seller", SELLER_ID, "seller@gmail.com", "password", "SELLER", new BigDecimal(4000), new ArrayList<>(), new ArrayList<>());
        when(userDao.findById(SELLER_ID)).thenReturn(Optional.of(seller));

        //Act & Assert: Người bán không được đặt giá
        assertThrows(RuntimeException.class, () -> service.placeBid(AUCTION_ID, SELLER_ID, new BigDecimal("2000")));
        verify(publisher, never()).publish(any());
    }


    //openAuction - Kiểm thử mở phòng đấu giá
    @Test
    @DisplayName("Test openAuction - mở thành công từ OPEN sang RUNNING và phát AUCTION_STARTED event")
    void openAuction_success() throws Exception {
        //Arrange: giả lập phiên đấu giá đang OPEN, ở trong danh sách chờ mở
        Auction open = buildAuction(AuctionStatus.OPEN);
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(open));
        when(auctionDao.update(any())).thenReturn(open);

        //Act: Kích hoạt mở phiên
        service.openAuction(AUCTION_ID);

        //Assert: trạng thái chuyển thành running và phát sự kiện đã mở phiên đấu giá
        assertEquals(AuctionStatus.RUNNING, open.getStatus());
        verify(publisher, times(1)).publish(any());
    }
    
    @Test
    @DisplayName("Test openAuction - phiên đang chạy thì ném RuntimeException, không phát sự kiện")
    void openAuction_alreadyRunning() throws Exception {
        //Arrange:giả lập phiên đấu giá đang RUNNING
        Auction running = buildAuction(AuctionStatus.RUNNING);
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(running));

        //Act & Assert: không được mở lại phòng đấu giá đang chạy
        assertThrows(RuntimeException.class, () -> service.openAuction(AUCTION_ID));
        verify(publisher, never()).publish(any());
    }

    @Test
    @DisplayName("Test openAuction - phiên đấu giá đã kết thúc thì ném exception")
    void openAuction_alreadyFinished() throws Exception {
        //Arrange: giả lập phiên đấu giá đã kết thúc
        Auction finished = buildAuction(AuctionStatus.FINISHED);
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(finished));

        //Act & Assert: Không được phép mở lại phiên đấu giá đã kết thúc thành công
        assertThrows(RuntimeException.class, () -> service.openAuction(AUCTION_ID));
    }

    @Test
    @DisplayName("Test openAuction - phiên đấu giá bị hủy")
    void openAuction_cancelled() throws Exception {
        //Arrange: giả lập phiên đấu giá bị hủy
        Auction cancelled = buildAuction(AuctionStatus.CANCELLED);
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(cancelled));
        
        //Act & Assert: không được kích hoạt phiên đấu giá đã bị hủy
        assertThrows(RuntimeException.class, () -> service.openAuction(AUCTION_ID));
    }

    @Test
    @DisplayName("Test openAuction - không có phiên đấu giá đang chờ mờ") 
    //Arrange: giả lập không tìm thấy phiên đấu giá nào đang chờ mở với ID mẫu
    void openAuction_notFound() throws Exception {
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.empty());
    
        //Act & Assert: không tìm thấy phiên đấu giá để mở -> ném lỗi
        assertThrows(RuntimeException.class, () -> service.openAuction(AUCTION_ID));
    }


    //closeAuction - Kiểm thử đóng phiên đấu giá hợp lệ
    @Test
    @DisplayName("Test closeAuction — đóng phiên thành công từ RUNNING chuyển sang FINISHED")
    void closeAuction_successFromRunning() throws Exception {
        // Arrange: Phiên đấu giá đang chạy bình thường và hết giờ
        Auction running = buildAuction(AuctionStatus.RUNNING);
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(running));
        when(auctionDao.update(any())).thenReturn(running);

        // Act: Tiến hành đóng phiên
        service.closeAuction(AUCTION_ID);

        // Assert: Chuyển dịch trạng thái thành công về FINISHED
        assertEquals(AuctionStatus.FINISHED, running.getStatus());
    }

    @Test
    @DisplayName("Test closeAuction — đóng phiên từ OPEN thành FINISHED")
    void closeAuction_successFromOpen() throws Exception {
        // Arrange: Phiên đang ở danh sách chờ nhưng chủ sàn không muốn đấu giá nữa -> đóng phiên sớm trước khi nó mở
        Auction open = buildAuction(AuctionStatus.OPEN);
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(open));
        when(auctionDao.update(any())).thenReturn(open);

        // Act: Đóng phiên
        service.closeAuction(AUCTION_ID);

        // Assert: Chuyển dịch về FINISHED thành công
        assertEquals(AuctionStatus.FINISHED, open.getStatus());
    }

    @Test
    @DisplayName("Test closeAuction — phiên đã FINISHED thì ném exception")
    void closeAuction_alreadyFinished_throwsException() {
        // Arrange: Phiên đã kết thúc từ trước
        when(auctionDao.findById(AUCTION_ID))
                .thenReturn(Optional.of(buildAuction(AuctionStatus.FINISHED)));

        // Act & Assert: Không được phép thực hiện đóng một phiên đấu giá lặp đi lặp lại
        assertThrows(RuntimeException.class, () -> service.closeAuction(AUCTION_ID));
    }

    @Test
    @DisplayName("Test closeAuction — phiên CANCELLED thì ném exception")
    void closeAuction_cancelled_throwsException() {
        // Arrange: Phiên đã mang trạng thái hủy bỏ
        when(auctionDao.findById(AUCTION_ID))
                .thenReturn(Optional.of(buildAuction(AuctionStatus.CANCELLED)));

        // Act & Assert: Trạng thái hủy là trạng thái cuối, cấm cập nhật đè sang trạng thái FINISHED
        assertThrows(RuntimeException.class,
                () -> service.closeAuction(AUCTION_ID));
    }



    //cancelAuction - Kiểm thử hủy phiên đấu giá
    @Test
    @DisplayName("Test cancelAuction - hủy phiên đấu giá thành công từ running")
    void cancelAuction_successFromRunning() throws Exception {
        //Arrange: giả lập phiên đang chạy phát hiện hàng giả, admin can thiệp hủy
        Auction running = buildAuction(AuctionStatus.RUNNING);
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(running));
        when(auctionDao.update(any())).thenReturn(running);

        //Act: Kích hoạt hủy phiên
        service.cancelAuction(AUCTION_ID);

        //Assert: trạng thái chuyển thành CANCELLED và phát sự kiện đã hủy phiên đấu giá
        assertEquals(AuctionStatus.CANCELLED, running.getStatus());
    }
    
    @Test
    @DisplayName("Test cancelAuction - hủy phiên đấu từ open")
    void cancelAuction_successFromOpen() throws Exception {
        Auction open = buildAuction(AuctionStatus.OPEN);
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(open));
        when(auctionDao.update(any())).thenReturn(open);

        service.cancelAuction(AUCTION_ID);

        assertEquals(AuctionStatus.CANCELLED, open.getStatus());
    }

    @Test
    @DisplayName("Test cancelAuction - phiên đã kết thúc")
    void canceledAuction_auctionFinished() {
        //Arrange: phiên đấu giá đã kết thúc 
        Auction finished = buildAuction(AuctionStatus.FINISHED);
        when(auctionDao.findById(AUCTION_ID)).thenReturn(Optional.of(finished));

        //Act & Assert: Giao dịch đã hoàn tất, không được hủy phiên 
        assertThrows(RuntimeException.class, () -> service.cancelAuction(AUCTION_ID));
    }

    @Test
    @DisplayName("cancelAuction — phiên đã CANCELLED")
    void cancelAuction_alreadyCancelled_throwsException() {
        // Arrange: Phiên đã nằm trong danh sách hủy bỏ
        when(auctionDao.findById(AUCTION_ID))
                .thenReturn(Optional.of(buildAuction(AuctionStatus.CANCELLED)));

        // Act & Assert: Từ chối xử lý lệnh hủy phiên lặp lại
        assertThrows(RuntimeException.class, () -> service.cancelAuction(AUCTION_ID));
    }

    //updateAuction - kiểm thử cập nhật phiên đấu giá
    @Test
    @DisplayName("updateAuction- cập nhật thông tin phiên đấu giá thành công và và trả về dữ liệu mới")
    void updateAuction_success_returnsUpdatedAuction() throws Exception {
        // Arrange: Chuẩn bị thực thể chứa thông tin mới - tăng giá hiện tại lên thành 7000 đô)
        Auction updated = buildAuction(AuctionStatus.RUNNING);
        updated.setCurrent_price(new BigDecimal("7000"));
        when(auctionDao.update(updated)).thenReturn(updated);

        // Act: Thực hiện lưu cập nhật qua Service
        Auction result = service.updateAuction(updated);

        // Assert: Đảm bảo dữ liệu nhận lại trùng khớp thông tin mới và Service ủy quyền (delegate) chuẩn xuống tầng DAO xử lý
        assertNotNull(result);
        assertEquals(0, new BigDecimal("7000").compareTo(result.getCurrent_price()));
        verify(auctionDao, times(1)).update(updated);
    }
}