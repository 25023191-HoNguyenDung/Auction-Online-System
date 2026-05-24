package service;

import com.auction.common.exception.ValidRegisterException;
import com.auction.server.dao.UserDao;
import com.auction.server.model.User;
import com.auction.server.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDao userDao;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userDao);
    }

    // Helper class để test vì User là abstract
    static class TestUser extends User {
        public TestUser(String user_name, long ID, String email, String password, String role) {
            super(user_name, ID, email, password, role);
        }

        @Override
        public void set_role() {
            this.role = "USER";
        }
    }

    @Test
    void register_Success() {
        TestUser user = new TestUser("@testuser123", 1L, "test@example.com", "", "USER");
        String rawPassword = "Password123!";

        when(userDao.findByUsername("@testuser123")).thenReturn(Optional.empty());
        when(userDao.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.register(user, rawPassword));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser.get_password());
        assertNotEquals(rawPassword, savedUser.get_password()); // đã được hash
    }

    @Test
    void register_InvalidUsername_ThrowsException() {
        TestUser user = new TestUser("invaliduser", 1L, "test@example.com", "", "USER"); // thiếu @
        String rawPassword = "Password123!";

        ValidRegisterException exception = assertThrows(ValidRegisterException.class,
                () -> authService.register(user, rawPassword));

        assertEquals("Username must start with '@' and contain both letters and numbers!", 
                    exception.getMessage());
    }

    @Test
    void register_InvalidPassword_ThrowsException() {
        TestUser user = new TestUser("@testuser123", 1L, "test@example.com", "", "USER");
        String rawPassword = "weakpass"; // thiếu số và ký tự đặc biệt

        ValidRegisterException exception = assertThrows(ValidRegisterException.class,
                () -> authService.register(user, rawPassword));

        assertEquals("Password must be at least 8 characters long and include letters, digits, and special characters!", 
                    exception.getMessage());
    }

    @Test
    void register_UsernameAlreadyExists_ThrowsException() {
        TestUser user = new TestUser("@testuser123", 1L, "test@example.com", "", "USER");
        String rawPassword = "Password123!";

        when(userDao.findByUsername("@testuser123")).thenReturn(Optional.of(user));

        ValidRegisterException exception = assertThrows(ValidRegisterException.class,
                () -> authService.register(user, rawPassword));

        assertEquals("Username already exists!", exception.getMessage());
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        TestUser user = new TestUser("@testuser123", 1L, "test@example.com", "", "USER");
        String rawPassword = "Password123!";

        when(userDao.findByUsername("@testuser123")).thenReturn(Optional.empty());
        when(userDao.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        ValidRegisterException exception = assertThrows(ValidRegisterException.class,
                () -> authService.register(user, rawPassword));

        assertEquals("Email already exists!", exception.getMessage());
    }

    @Test
    void login_Success() {
        String username = "@testuser123";
        String rawPassword = "Password123!";
        String hashedPassword = invokePrivateHash(rawPassword); // SHA-256 của Password123!

        TestUser user = new TestUser(username, 1L, "test@example.com", hashedPassword, "USER");

        when(userDao.findByUsername(username)).thenReturn(Optional.of(user));

        User result = assertDoesNotThrow(() -> authService.login(username, rawPassword));

        assertEquals(user, result);
    }

    @Test
    void login_UsernameNotFound_ThrowsException() {
        String username = "@nonexistent";

        when(userDao.findByUsername(username)).thenReturn(Optional.empty());

        ValidRegisterException exception = assertThrows(ValidRegisterException.class,
                () -> authService.login(username, "anyPassword"));

        assertEquals("Username does not exist! Please register first!", exception.getMessage());
    }

    @Test
    void login_IncorrectPassword_ThrowsException() {
        String username = "@testuser123";
        String correctPassword = "Password123!";
        String wrongPassword = "WrongPass123!";
        String hashedPassword = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3";

        TestUser user = new TestUser(username, 1L, "test@example.com", hashedPassword, "USER");

        when(userDao.findByUsername(username)).thenReturn(Optional.of(user));

        ValidRegisterException exception = assertThrows(ValidRegisterException.class,
                () -> authService.login(username, wrongPassword));

        assertEquals("Incorrect password!Try again!", exception.getMessage());
    }

    @Test
    void hashPassword_ConsistentHashing() {
        String password = "TestPass123!";
        
        // Gọi 2 lần để kiểm tra hash nhất quán
        String hash1 = invokePrivateHash(password);
        String hash2 = invokePrivateHash(password);

        assertEquals(hash1, hash2);
        assertNotEquals(password, hash1); // phải là hash, không phải plain text
    }

    // Helper method để test private method hashPassword
    private String invokePrivateHash(String password) {
        try {
            var method = AuthService.class.getDeclaredMethod("hashPassword", String.class);
            method.setAccessible(true);
            return (String) method.invoke(authService, password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}