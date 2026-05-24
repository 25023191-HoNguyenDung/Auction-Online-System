package service;

import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.ItemDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Item;
import com.auction.server.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock private ItemDao    itemDao;
    @Mock private AuctionDao auctionDao;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemDao, auctionDao);
    }

    // Helper methods để tạo Item và Auction mẫu cho test
    private Item buildItem(long id) {
        return new Item(id, 100L, "iPhone 15", "Good phone",
                "ELECTRONICS", BigDecimal.TEN, BigDecimal.TEN, "url.jpg");
    }

    private Auction buildAuction(long itemId, AuctionStatus status) {
        Auction a = new Auction();
        a.setItem_id(itemId);
        a.setStatus(status);
        return a;
    }

    @Test
    void createItem_Success() {
        long sellerId = 100L;
        String itemName = "iPhone 15";
        BigDecimal price = new BigDecimal("15000000");

        Item savedItem = new Item(1L, sellerId, itemName, "desc",
                "ELECTRONICS", price, price, "img.jpg");
        when(itemDao.save(any(Item.class))).thenReturn(savedItem);

        Item result = itemService.createItem(sellerId, itemName, "desc",
                "ELECTRONICS", price, "img.jpg");

        assertNotNull(result);
        assertEquals(sellerId, result.getSellerId());
        assertEquals(itemName, result.getItemName());
        // currentPrice phải bằng startingPrice khi mới tạo
        assertEquals(0, price.compareTo(result.getCurrentPrice()));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemDao).save(captor.capture());
        assertEquals("ELECTRONICS", captor.getValue().getCategory());
    }

    @Test
    void createItem_InvalidName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                itemService.createItem(100L, "   ", "desc",
                        "ELECTRONICS", BigDecimal.TEN, "url"));
        verify(itemDao, never()).save(any());
    }

    @Test
    void createItem_NullName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                itemService.createItem(100L, null, "desc",
                        "ELECTRONICS", BigDecimal.TEN, "url"));
        verify(itemDao, never()).save(any());
    }

    @Test
    void createItem_InvalidPrice_Zero_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                itemService.createItem(100L, "Item", "desc",
                        "ELECTRONICS", BigDecimal.ZERO, "url"));
        verify(itemDao, never()).save(any());
    }

    @Test
    void createItem_InvalidPrice_Negative_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                itemService.createItem(100L, "Item", "desc",
                        "ELECTRONICS", new BigDecimal("-1"), "url"));
        verify(itemDao, never()).save(any());
    }

    // Tạo item 
    @Test
    void createTypedItem_Art_SavesItem() {
        long sellerId = 100L;
        BigDecimal price = new BigDecimal("50000000");

        // Giả lập itemDao.save trả về item bất kỳ
        when(itemDao.save(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            i.setItemId(99L);
            return i;
        });

        Item result = itemService.createTypedItem(
                sellerId, "Painting", "Famous art",
                "ART", price, "art.jpg",
                "Picasso", "Modern");

        assertNotNull(result);
        assertEquals(99L, result.getItemId());
        verify(itemDao).save(any(Item.class));
    }

    @Test
    void createTypedItem_InvalidCategory_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                itemService.createTypedItem(
                        100L, "Item", "desc",
                        "INVALID_CATEGORY", BigDecimal.TEN, "url"));
        verify(itemDao, never()).save(any());
    }

    // Lấy item theo id
    @Test
    void getById_Success() {
        Item item = buildItem(5L);
        when(itemDao.findById(5L)).thenReturn(Optional.of(item));

        Item result = itemService.getById(5L);

        assertEquals(5L, result.getItemId());
        verify(itemDao).findById(5L);
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(itemDao.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> itemService.getById(999L));

        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    void findById_ReturnsOptionalPresent() {
        when(itemDao.findById(10L)).thenReturn(Optional.of(buildItem(10L)));
        assertTrue(itemService.findById(10L).isPresent());
    }

    @Test
    void findById_NotFound_ReturnsEmpty() {
        when(itemDao.findById(10L)).thenReturn(Optional.empty());
        assertTrue(itemService.findById(10L).isEmpty());
    }

    // Tìm item theo seller
    @Test
    void findBySeller_ReturnsList() {
        when(itemDao.findBySellerId(100L))
                .thenReturn(List.of(buildItem(1L), buildItem(2L)));

        List<Item> result = itemService.findBySeller(100L);

        assertEquals(2, result.size());
        verify(itemDao).findBySellerId(100L);
    }

    @Test
    void getAllItems_ReturnsAll() {
        when(itemDao.findAll())
                .thenReturn(List.of(buildItem(1L), buildItem(2L), buildItem(3L)));

        assertEquals(3, itemService.getAllItems().size());
    }


    @Test
    void updateItem_Success() {
        Item existing = new Item(1L, 100L, "Old Name", "Old Desc",
                "ELECTRONICS", BigDecimal.TEN, BigDecimal.TEN, "old.jpg");

        when(itemDao.findById(1L)).thenReturn(Optional.of(existing));
        when(auctionDao.findAll()).thenReturn(List.of()); // không có auction active
        when(itemDao.update(any(Item.class))).thenReturn(existing);

        Item result = itemService.updateItem(1L, "New Name", "New Desc", "new.jpg");

        assertEquals("New Name",  result.getItemName());
        assertEquals("New Desc",  result.getDescription());
        assertEquals("new.jpg",   result.getImageUrl());
        verify(itemDao).update(existing);
    }

    @Test
    void updateItem_OnlyName_OtherFieldsUnchanged() {
        Item existing = new Item(1L, 100L, "Old Name", "Old Desc",
                "ELECTRONICS", BigDecimal.TEN, BigDecimal.TEN, "old.jpg");

        when(itemDao.findById(1L)).thenReturn(Optional.of(existing));
        when(auctionDao.findAll()).thenReturn(List.of());
        when(itemDao.update(any(Item.class))).thenReturn(existing);

        Item result = itemService.updateItem(1L, "New Name", null, null);

        assertEquals("New Name", result.getItemName());
        assertEquals("Old Desc", result.getDescription()); // không đổi
        assertEquals("old.jpg",  result.getImageUrl());    // không đổi
    }

    @Test
    void updateItem_HasActiveAuction_ThrowsException() {
        Item item = buildItem(1L);
        Auction active = buildAuction(1L, AuctionStatus.RUNNING);

        when(itemDao.findById(1L)).thenReturn(Optional.of(item));
        when(auctionDao.findAll()).thenReturn(List.of(active));

        // FIX: itemDao.update KHÔNG được gọi vì exception xảy ra trước
        assertThrows(IllegalStateException.class,
                () -> itemService.updateItem(1L, "New Name", null, null));

        verify(itemDao, never()).update(any());
    }

    @Test
    void updateItem_AuctionOpen_ThrowsException() {
        Item item = buildItem(1L);
        Auction open = buildAuction(1L, AuctionStatus.OPEN);

        when(itemDao.findById(1L)).thenReturn(Optional.of(item));
        when(auctionDao.findAll()).thenReturn(List.of(open));

        assertThrows(IllegalStateException.class,
                () -> itemService.updateItem(1L, "New Name", null, null));
    }

    // ── deleteItem ────────────────────────────────────────────
    // FIX QUAN TRỌNG: deleteItem() KHÔNG gọi findById() — chỉ cần stub auctionDao.findAll() và itemDao.deleteById()

    @Test
    void deleteItem_Success() {
        // KHÔNG cần stub itemDao.findById — deleteItem không gọi nó
        when(auctionDao.findAll()).thenReturn(List.of());
        when(itemDao.deleteById(1L)).thenReturn(true);

        boolean result = itemService.deleteItem(1L);

        assertTrue(result);
        verify(itemDao).deleteById(1L);
        verify(itemDao, never()).findById(anyLong()); // xác nhận findById không được gọi
    }

    @Test
    void deleteItem_NotFound_ReturnsFalse() {
        when(auctionDao.findAll()).thenReturn(List.of());
        when(itemDao.deleteById(99L)).thenReturn(false);

        boolean result = itemService.deleteItem(99L);

        assertFalse(result);
    }

    @Test
    void deleteItem_HasActiveAuction_Running_ThrowsException() {
        // KHÔNG cần stub itemDao.findById — deleteItem không gọi nó
        Auction active = buildAuction(1L, AuctionStatus.RUNNING);
        when(auctionDao.findAll()).thenReturn(List.of(active));

        assertThrows(IllegalStateException.class, () -> itemService.deleteItem(1L));

        verify(itemDao, never()).deleteById(anyLong()); // xác nhận deleteById không được gọi
    }

    @Test
    void deleteItem_HasActiveAuction_Open_ThrowsException() {
        Auction open = buildAuction(1L, AuctionStatus.OPEN);
        when(auctionDao.findAll()).thenReturn(List.of(open));

        assertThrows(IllegalStateException.class, () -> itemService.deleteItem(1L));

        verify(itemDao, never()).deleteById(anyLong());
    }

    @Test
    void deleteItem_AuctionFinished_AllowsDelete() {
        // Auction đã FINISHED → không còn "active" → được phép xóa
        Auction finished = buildAuction(1L, AuctionStatus.FINISHED);
        when(auctionDao.findAll()).thenReturn(List.of(finished));
        when(itemDao.deleteById(1L)).thenReturn(true);

        assertTrue(itemService.deleteItem(1L));
        verify(itemDao).deleteById(1L);
    }
}