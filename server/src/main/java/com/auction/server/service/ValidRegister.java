package com.auction.server.service;

public class ValidRegister {
   
    public static boolean isValidUsername(String username) {
    
        if (username == null || username.isEmpty()) return false;

        // Phải bắt đầu bằng "@"
        if (!username.startsWith("@")) return false;

        // Phần sau "@" phải có ít nhất 1 chữ cái và 1 chữ số
        String body = username.substring(1); // bỏ ký tự "@" đầu
        boolean hasLetter = body.matches(".*[a-zA-Z].*");
        boolean hasDigit  = body.matches(".*[0-9].*");

        return hasLetter && hasDigit;

    }

    public static boolean isValidPassword(String password) {

        if (password == null || password.length() < 8) return false;

        boolean hasLetter  = password.matches(".*[a-zA-Z].*");
        boolean hasDigit   = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?].*");

        return hasLetter && hasDigit && hasSpecial;
    }
    
}
