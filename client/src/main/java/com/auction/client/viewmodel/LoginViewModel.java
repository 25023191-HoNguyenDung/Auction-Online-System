package com.auction.client.viewmodel;

import com.auction.client.model.User;
import com.auction.client.sessions.UserSession;

/**
 * LoginViewModel
 * Owns all login business logic:
 *  - input validation
 *  - credential checking (mock — replace with server call later)
 *  - session management
 *
 * Controller only calls this and reacts to the result.
 */
public class LoginViewModel {

    // ── Result returned to controller ─────────────────────────
    public enum LoginResult {
        SUCCESS,
        EMPTY_FIELDS,
        INVALID_CREDENTIALS
    }

    private String errorMessage = "";

    // ── Main login method ─────────────────────────────────────
    /**
     * Attempt login with given credentials.
     * Returns a LoginResult the controller can act on.
     * When server is ready: replace the mock block with a network call.
     */
    public LoginResult login(String email, String password) {
        // 1. Validate inputs
        if (email == null || email.isBlank() ||
            password == null || password.isBlank()) {
            errorMessage = "Please fill in all fields.";
            return LoginResult.EMPTY_FIELDS;
        }

        // 2. Mock credential check
        //    TODO: replace with ServerConnection.login(email, password)
        User user = resolveMockUser(email.trim(), password);

        if (user == null) {
            errorMessage = "Invalid email or password.";
            return LoginResult.INVALID_CREDENTIALS;
        }

        // 3. Store in session
        UserSession.getInstance().login(user);
        errorMessage = "";
        return LoginResult.SUCCESS;
    }

    // ── Validation helpers ────────────────────────────────────
    public boolean isEmailValid(String email) {
        return email != null && !email.isBlank()
            && email.contains("@") && email.contains(".");
    }

    public boolean isPasswordValid(String password) {
        return password != null && password.length() >= 6;
    }

    // ── Error message ─────────────────────────────────────────
    public String getErrorMessage() {
        return errorMessage;
    }
    // ── Mock credentials ──────────────────────────────────────
    /**
     * Returns a User for known mock credentials, null otherwise.
     * Replace this entire method body with a server call when ready.
     */
    private User resolveMockUser(String email, String password) {
        return switch (email.toLowerCase()) {
            case "admin@auctionpro.com" ->
                password.equals("admin123")
                    ? new User(0L, "Administrator", email, "ADMIN") : null;
            case "collector@aureate.com" ->
                password.equals("password")
                    ? new User(1L, "@collector_a", email, "BIDDER") : null;
            case "seller@aureate.com" ->
                password.equals("seller123")
                    ? new User(2L, "@sterlinghouse", email, "SELLER") : null;
            default -> null;
        };
    }
}