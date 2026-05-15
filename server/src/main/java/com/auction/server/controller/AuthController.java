package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.model.Bidder;
import com.auction.server.model.Seller;
import com.auction.server.service.AuthService;
import com.auction.common.exception.ValidRegisterException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    public Map<String, Object> handleLogin(Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = (String) payload.get("username");
            String password = (String) payload.get("password");

            // Validate input cơ bản trước khi gọi service
            if (username == null || username.isBlank()) {
                return errorResponse("Username cannot be null or blank.");
            }
            if (password == null || password.isBlank()) {
                return errorResponse("Password cannot be null or blank.");
            }

            User user = authService.login(username, password);

            response.put("type", "LOGIN_RES");
            response.put("success", true);
            response.put("userId", user.get_ID());
            response.put("username", user.get_user_name());
            response.put("role", user.getRole());

        } catch (ValidRegisterException e) {
            // Sai username hoặc password thì báo lỗi rõ ràng cho client
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            return errorResponse("Server error during login: " + e.getMessage());
        }

        return response;
    }


    public Map<String, Object> handleRegister(Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = (String) payload.get("username");
            String password = (String) payload.get("password");
            String email    = (String) payload.get("email");
            String role     = (String) payload.get("role");

            if (username == null || password == null || email == null || role == null) {
                return errorResponse("missing required fields (username, password, email, role).");
            }

            User newUser = buildUser(username, email, password, role);

            
            authService.register(newUser, password);

            response.put("type", "REGISTER_RES");
            response.put("success", true);
            response.put("message", "Registration successful! Please login.");

        } catch (ValidRegisterException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            return errorResponse("Server error during registration: " + e.getMessage());
        }

        return response;
    }

    private User buildUser(String username, String email, String password, String role) {
        return switch (role.toUpperCase()) {
            case "BIDDER" -> new Bidder(username, 0L, email, password, "BIDDER",
                                        0.0, new ArrayList<>());
            case "SELLER" -> new Seller(username, 0L, email, password, "SELLER",
                                        0.0, new ArrayList<>(), new ArrayList<>());
            default -> throw new ValidRegisterException(
                    "Invalid role: " + role + ". Only BIDDER or SELLER are allowed.");
        };
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("type", "ERROR_RES");
        err.put("success", false);
        err.put("message", message);
        return err;
    }
}