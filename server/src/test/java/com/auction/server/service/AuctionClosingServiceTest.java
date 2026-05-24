package com.auction.server.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.Mock;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auction.common.exception.AuctionConnectException;
import com.auction.server.dao.AuctionDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;


@ExtendWith(MockitoExtension.class)
public class AuctionClosingServiceTest {
    //Các Dependency giả lập (Mocks) để chặn tương tác với DB thật
    @Mock
    private AuctionDao auctionDao;
    @Mock
    private AuctionService auctionService;
    //Đối tượng thực tế dùng test
    private AuctionClosingService closingService;
    
    
    //Dữ liệu mẫu
    //Tạo 1 phiên đấu giá giả lập đã hết hạn
    private Auction buildExpiredAuction(long id) {
        Auction a = new Auction();
        a.setId(id);
        a.setItem_id(1L);
        a.setSeller_id(2L);
        a.setStarting_price(new BigDecimal("1000000"));
        a.setCurrent_price(new BigDecimal("1000000"));
        a.setStatus(AuctionStatus.RUNNING);
        a.setStart_time(LocalDateTime.now().minusHours(2));
        a.setEnd_time(LocalDateTime.now().minusMinutes(5)); //Đảm bảo đã qua thời gian kết thúc
        a.setWinner_bidder_id(0L);  //Chưa có ai thắng
        return a;
    }
    //Tạo 1 phiên đấu giá giả lập đến giờ mở
    private Auction buildReadyToOpenAuction(long id) {
        Auction a = new Auction();
        a.setId(id);
        a.setItem_id(1L);
        a.setSeller_id(2L);
        a.setStarting_price(new BigDecimal("1000000"));
        a.setCurrent_price(new BigDecimal("1000000"));
        a.setStatus(AuctionStatus.OPEN); // Chờ mở
        a.setStart_time(LocalDateTime.now().minusMinutes(1));   //Đảm bảo qua thời gian mở 
        a.setEnd_time(LocalDateTime.now().plusHours(2));
        a.setWinner_bidder_id(0L);
        return a;
    }

    //Vòng đời test
    @BeforeEach
    void setUp() {
        // Thiết lập ban đầu trước mỗi test case:
        // Đặt chu kì quét là 1 giây
        closingService = new AuctionClosingService(auctionDao, auctionService, 1);
    }

    @AfterEach
    void tearDown() {
        // Dọn dẹp sau khi mỗi test case chạy xong:
        // Bắt buộc phải gọi stop() để tắt Thread ngầm của scheduler,
        if (closingService != null) {
            closingService.stop();
        }
    }

    //Constructor test
    @Test
    @DisplayName("Constructor khởi tạo đúng với interval được truyền vào")
    void constructor_withCustomInterval() {
        AuctionClosingService service = new AuctionClosingService(auctionDao, auctionService, 5);
        assertEquals(5, service.getCheckIntervalSeconds());
    }

    @Test
    @DisplayName("Constructor mặc định sẽ sử dụng interval là 10 giây")
    void constructor_withDefaultInterval() {
        AuctionClosingService service = new AuctionClosingService(auctionDao, auctionService);
        assertEquals(10, service.getCheckIntervalSeconds());
    }

    //processExpiredAuctions - Kiểm thử đóng các phiên hết hạn
    @Test
    @DisplayName("Test proccessExpiredAuctions - không có phiên hết hạn")
    void proccessExpiredAuctions_notFoundExpired() throws AuctionConnectException{
        //Arrange: giả lập không có phiên hết hạn, DB trả về danh sách rỗng
        when(auctionDao.findExpiredRunning()).thenReturn(Collections.emptyList());

        //Act: kích hoạt scheduler
        closingService.start();

        //Assert: dùng after(500) để chờ nửa giây, đảm bảo hàm closeAuction hoàn toàn không bị gọi nhầm
        verify(auctionService, after(500).never()).closeAuction(anyLong());
    }

    @Test
    @DisplayName("Test proccessExpiredAuctions - có 1 phiên hết hạn, closeAuction() được gọi đúng 1 lần cho ID đó")
    void proccessExpiredAuctions_oneExpiredAuctions () throws AuctionConnectException {
        //Arrange: giả lập có 1 phiên ID = 10 cần đóng
        Auction expired = buildExpiredAuction(10L);
        when(auctionDao.findExpiredRunning()).thenReturn(List.of(expired));

        //Act: kích hoạt khởi động vòng quét của scheduler
        closingService.start();

        //Assert: quét và đợi tối đa 1.5 giây. gọi closeAuction(10L) đúng 1 lần
        verify(auctionService, timeout(1500).times(1)).closeAuction(10L);
    }

    @Test
    @DisplayName("Test proccessExpiredAuctions- có nhiều phiên hết hạn, tất cả ID đều phải được gọi để đóng")
    void proccessExpiredAuctions_multipleExpiredAuctions() throws AuctionConnectException {
        //Arrange: giả lập có 3 phiên hết hạn
        Auction a1 = buildExpiredAuction(1L);
        Auction a2 = buildExpiredAuction(2L);
        Auction a3 = buildExpiredAuction(3L);
        when(auctionDao.findExpiredRunning()).thenReturn(List.of(a1, a2, a3));

        //Act: kích hoạt khởi động vòng quét của scheduler
        closingService.start();

        //Assert: quét và đợi tối đa 1.5 giây. Đảm bảo toàn bộ danh sách ID đều đã được gửi lệnh yêu cầu đóng thành công
        verify(auctionService, timeout(1500)).closeAuction(1L);
        verify(auctionService, timeout(1500)).closeAuction(2L);
        verify(auctionService, timeout(1500)).closeAuction(3L);
    }

    @Test
    @DisplayName("Test proccessExpiredAuctions - quá trình đóng 1 phiên bị ném lỗi Exception, vẫn đóng các phiên khác bình thường")
    void proccessExpiredAuctions_oneThrowsException() throws AuctionConnectException {
        //Arrange: giả lập có 2 phiên hết hạn
        Auction a1 = buildExpiredAuction(11L);
        Auction a2 = buildExpiredAuction(12L);
        when(auctionDao.findExpiredRunning()).thenReturn(List.of(a1, a2));
        //Giả lập cố tình ném lỗi khi hệ thống cố gắng đóng phiên 11
        doThrow(new RuntimeException("DB error")).when(auctionService).closeAuction(11L);

        //Act & Assert 1: đảm bảo toàn bộ hàm start() không bị crash (văng lỗi ra ngoài)
        assertDoesNotThrow(() -> closingService.start());
        //Assert: phiên số 12 vẫn được xử lý an toàn
        verify(auctionService, timeout(1500)).closeAuction(12L);
    }


    //processReadyToOpenAuctions — Kiểm thử mở các phiên đến giờ
    @Test
    @DisplayName("Test processReadyToOpenAuctions - không có phiên chờ mở")
    void processReadyToOpenAuctions_noReadyAuctions() throws AuctionConnectException{
        //Arrange: giả lập không có phiên nào chờ mở, DB trả về danh sách rỗng
        when(auctionDao.findOpenReadyToStart()).thenReturn(Collections.emptyList());

        //Act: kích hoạt khởi động vòng quét của scheduler
        closingService.start();

        //Assert: đảm bảo hàm openAuction không bị tác động
        verify(auctionService, after(500).never()).openAuction(anyLong());
    }

    @Test
    @DisplayName("Test processReadyToOpenAuctions - có 1 phiên chờ mở, openAuction() được gọi đúng cho ID đó")
    void processReadyToOpenAuctions_oneReadyAuction() throws AuctionConnectException {
        //Arrange: chuẩn bị 1 đấu giá trạng thái OPEN đã đến giờ lên sàn (ID = 20L)
        Auction ready = buildReadyToOpenAuction(20L);
        when(auctionDao.findOpenReadyToStart()).thenReturn(List.of(ready));

        //Act: kích hoạt khởi động vòng quét của scheduler
        closingService.start();

        //Assert: xác nhận hệ thống đã thực hiện lệnh mở phiên cho ID 20L
        verify(auctionService, timeout(1500)).openAuction(20L);
    }

    @Test
    @DisplayName("Test processReadyToOpenAuctions - quá trình mở 1 phiên bị lỗi, phiên khác vẫn mở")
    void processReadyToOpenAuctions_openThrowsException() throws AuctionConnectException {
        //Arrange: chuẩn bị 2 phiên đến giờ mở, thiết lập phiên số 31 ném ra lỗi kiểm tra (Checked Exception)
        Auction a1 = buildReadyToOpenAuction(31L);
        Auction a2 = buildReadyToOpenAuction(32L);
        when(auctionDao.findOpenReadyToStart()).thenReturn(List.of(a1, a2));
        doThrow(new AuctionConnectException("DB error")).when(auctionService).openAuction(31L);

        //Act: kích hoạt ứng dụng và kiểm soát không cho văng ngoại lệ ra ngoài luồng chính
        assertDoesNotThrow(() -> closingService.start());

        //Assert: đảm bảo phiên số 32 tiếp sau đó vẫn được mở bình thường
        verify(auctionService, timeout(1500)).openAuction(32L);
    }

    
    //Kiểm thử vòng đời Scheduler 
    @Test
    @DisplayName("Khi gọi start(), scheduler phải kích hoạt và quét Database ngay lập tức (không delay)")
    void start_schedulerRunsImmediately() {
        //Arrange: thiết lập môi trường rỗng ban đầu cho DB
        when(auctionDao.findExpiredRunning()).thenReturn(Collections.emptyList());
        when(auctionDao.findOpenReadyToStart()).thenReturn(Collections.emptyList());

        //Act: kích hoạt khởi động vòng quét của scheduler
        closingService.start();

        //Assert: do initialDelay = 0 nên DAO phải bị truy vấn ngay lập tức, kiểm tra xem nó có gọi ít nhất 1 lần không
        verify(auctionDao, timeout(1500).atLeastOnce()).findExpiredRunning();
        verify(auctionDao, timeout(1500).atLeastOnce()).findOpenReadyToStart();
    }

    @Test
    @DisplayName("Khi gọi stop(), scheduler bị triệt tiêu hoàn toàn, ngưng việc gọi DB")
    void stop_schedulerRemoveCompletely() throws InterruptedException {
        //Arrange: thiết lập giả lập DB rỗng
        when(auctionDao.findExpiredRunning()).thenReturn(Collections.emptyList());
        when(auctionDao.findOpenReadyToStart()).thenReturn(Collections.emptyList());

        //Act: kích hoạt khởi động vòng quét của scheduler, chạy chu kì đầu, đếm số lần gọi
        closingService.start();
        verify(auctionDao, timeout(1500).atLeastOnce()).findExpiredRunning();  
        int callsBeforeStop = mockingDetails(auctionDao).getInvocations().size(); //Đếm số lần gọi trước khi stop
        closingService.stop(); //Phát lệnh đóng luồng ngầm hoàn toàn
        Thread.sleep(1500); //Cố tình ngủ đông 1.5 giây (dài hơn chu kỳ interval 1s) để xem thread cũ có tự ý chạy không
        int callsAfterStop = mockingDetails(auctionDao).getInvocations().size(); //Đếm lại số lần gọi sau thời gian chờ

        //Assert: kiểm chứng tổng số lần tương tác tới DAO phải bằng nhau, chứng tỏ luồng đã chết hẳn và không sinh thêm cuộc gọi mới
        assertEquals(callsBeforeStop, callsAfterStop,
                "Error: Scheduler continues to call the database after stop() is called - Thread leak detected!");
    }

    @Test
    @DisplayName("Đảm bảo Scheduler quét và xử lý cả mở phiên và đóng phiên đồng thời trong cùng 1 chu kỳ")
    void bothOpenAndClose() throws AuctionConnectException {
        //Arrange: thiết lập DB vừa có 1 phiên cần mở (40L), vừa có 1 phiên cần đóng (41L)
        Auction toOpen = buildReadyToOpenAuction(40L);
        Auction toClose = buildExpiredAuction(41L);
        when(auctionDao.findOpenReadyToStart()).thenReturn(List.of(toOpen));
        when(auctionDao.findExpiredRunning()).thenReturn(List.of(toClose));

        //Act: kích hoạt khởi động vòng quét của scheduler
        closingService.start();

        //Assert: đảm bảo cả hai hành vi nghiệp vụ độc lập đều được xử lý trong cùng một vòng quét lịch trình
        verify(auctionService, timeout(1500)).closeAuction(41L);
        verify(auctionService, timeout(1500)).openAuction(anyLong());
    }
}
