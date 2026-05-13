package com.auction.server.service;

import com.auction.server.dao.UserDao;

import com.auction.server.model.User;
import com.auction.common.exception.ValidRegisterException;

// server/src/main/java/com/auction/server/service/AuthService.java
public class AuthService {

    private final UserDao UserDao;

    public AuthService(UserDao UserDao) {
        this.UserDao = UserDao;
    }

public void register(User newUser, String rawPassword) {

    // username phải hợp lệ, password phải hợp lệ
    if (!ValidRegister.isValidUsername(newUser.get_user_name()))
        throw new ValidRegisterException("Invalid username!");

    if (!ValidRegister.isValidPassword(rawPassword))
        throw new ValidRegisterException("Invalid password!");

    // Check trùng username
    if (UserDao.findByUsername(newUser.get_user_name()) != null)
        throw new ValidRegisterException("Username already exists!");

    // Hash password rồi set lại
    newUser.set_password(hashPassword(rawPassword));

    // Lưu vào DB
    UserDao.save(newUser);
}

    private String hashPassword(String password) {
        // Dùng SHA-256
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // Chuyển byte array thành hex string
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
