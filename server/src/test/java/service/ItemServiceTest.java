package service;

import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.ItemDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Item;
import com.auction.server.pattern.ItemFactory;
import com.auction.server.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTestGk {

    @Mock
    private ItemDao itemDao;

    @Mock
    private AuctionDao auctionDao;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemDao, auctionDao);
    }

    @Test
    void createItem_Success() {
        long sellerId = 100L;
        String itemName = "iPhone 15";
        String description = "New phone";
        String category = "ELECTRONICS";
        BigDecimal startingPrice = new BigDecimal("15000000");
        String imageUrl = "https://example.com/iphone.jpg";

        Item savedItem = new Item(1L, sellerId, itemName, description, category, startingPrice, startingPrice, imageUrl);
        when(itemDao.save(any(Item.class))).thenReturn(savedItem);

        Item result = itemService.createItem(sellerId, itemName, description, category, startingPrice, imageUrl);

        assertNotNull(result);
        assertEquals(sellerId, result.getSellerId());
        assertEquals(itemName, result.getItemName());
        assertEquals(startingPrice, result.getStartingPrice());
        assertEquals(startingPrice, result.getCurrentPrice());

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemDao).save(captor.capture());

        Item captured = captor.getValue();
        assertEquals(category.toUpperCase(), captured.getCategory());
    }

    @Test
    void createItem_InvalidName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                itemService.createItem(100L, "   ", "desc", "ELECTRONICS", BigDecimal.TEN, "url"));
    }

    @Test
    void createItem_InvalidPrice_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                itemService.createItem(100L, "Item", "desc", "ELECTRONICS", BigDecimal.ZERO, "url"));
    }

    @Test
    void createTypedItem_Success() {
        long sellerId = 100L;
        String itemName = "Painting";
        String description = "Famous art";
        String category = "ART";
        BigDecimal price = new BigDecimal("50000000");
        String imageUrl = "art.jpg";

        // Mock ItemFactory
        Item mockArt = new Item(1L, sellerId, itemName, description, "ART", price, price, imageUrl);
        try (MockedStatic<ItemFactory> mockedFactory = mockStatic(ItemFactory.class)) {
            mockedFactory.when(() -> ItemFactory.createItem(anyString(), anyLong(), anyLong(), anyString(), anyString(),
                    any(BigDecimal.class), any(BigDecimal.class), anyString(), any()))
                    .thenReturn(mockArt);

            when(itemDao.save(any(Item.class))).thenReturn(mockArt);

            Item result = itemService.createTypedItem(sellerId, itemName, description, category, price, imageUrl, "Picasso", "Modern");

            assertNotNull(result);
            verify(itemDao).save(any(Item.class));
        }
    }

    @Test
    void getById_Success() {
        Item item = new Item(5L, 100L, "Laptop", "Good laptop", "ELECTRONICS", BigDecimal.valueOf(20000000), BigDecimal.valueOf(20000000), "url");
        when(itemDao.findById(5L)).thenReturn(Optional.of(item));

        Item result = itemService.getById(5L);

        assertEquals(5L, result.getItemId());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(itemDao.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> itemService.getById(999L));

        assertEquals("Item not found, id=999", exception.getMessage());
    }

    @Test
    void findById_ReturnsOptional() {
        Item item = new Item();
        when(itemDao.findById(10L)).thenReturn(Optional.of(item));

        Optional<Item> result = itemService.findById(10L);

        assertTrue(result.isPresent());
    }

    @Test
    void findBySeller_ReturnsList() {
        List<Item> items = List.of(new Item(), new Item());
        when(itemDao.findBySellerId(100L)).thenReturn(items);

        List<Item> result = itemService.findBySeller(100L);

        assertEquals(2, result.size());
    }

    @Test
    void getAllItems_ReturnsAll() {
        List<Item> allItems = List.of(new Item(), new Item(), new Item());
        when(itemDao.findAll()).thenReturn(allItems);

        List<Item> result = itemService.getAllItems();

        assertEquals(3, result.size());
    }

    @Test
    void updateItem_Success() {
        Item existingItem = new Item(1L, 100L, "Old Name", "Old Desc", "ELECTRONICS", BigDecimal.TEN, BigDecimal.TEN, "old.jpg");
        when(itemDao.findById(1L)).thenReturn(Optional.of(existingItem));
        when(auctionDao.findAll()).thenReturn(List.of()); // No active auction
        when(itemDao.update(any(Item.class))).thenReturn(existingItem);

        Item updated = itemService.updateItem(1L, "New Name", "New Description", "new.jpg");

        assertEquals("New Name", updated.getItemName());
        assertEquals("New Description", updated.getDescription());
        assertEquals("new.jpg", updated.getImageUrl());
    }

    @Test
    void updateItem_HasActiveAuction_ThrowsException() {
        Item item = new Item(1L, 100L, "Item", "", "", BigDecimal.TEN, BigDecimal.TEN, "");
        Auction activeAuction = new Auction();
        activeAuction.setItem_id(1L);
        activeAuction.setStatus(AuctionStatus.RUNNING);

        when(itemDao.findById(1L)).thenReturn(Optional.of(item));
        when(auctionDao.findAll()).thenReturn(List.of(activeAuction));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> itemService.updateItem(1L, "New Name", null, null));

        assertTrue(exception.getMessage().contains("active auction"));
    }

    @Test
    void deleteItem_Success() {
        Item item = new Item(1L, 100L, "Item", "", "", BigDecimal.TEN, BigDecimal.TEN, "");
        when(itemDao.findById(1L)).thenReturn(Optional.of(item));
        when(auctionDao.findAll()).thenReturn(List.of()); // No active auction
        when(itemDao.deleteById(1L)).thenReturn(true);

        boolean result = itemService.deleteItem(1L);

        assertTrue(result);
        verify(itemDao).deleteById(1L);
    }

    @Test
    void deleteItem_HasActiveAuction_ThrowsException() {
        Item item = new Item(1L, 100L, "Item", "", "", BigDecimal.TEN, BigDecimal.TEN, "");
        Auction auction = new Auction();
        auction.setItem_id(1L);
        auction.setStatus(AuctionStatus.OPEN);

        when(itemDao.findById(1L)).thenReturn(Optional.of(item));
        when(auctionDao.findAll()).thenReturn(List.of(auction));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> itemService.deleteItem(1L));

        assertTrue(exception.getMessage().contains("active auction"));
    }
    
}
