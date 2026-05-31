package com.auction.server.model;

public abstract class User {
    
    //Call Fields:
    private String user_name;
    private long ID;
    private String email;
    private String password;
    protected String role;

    public String getRole() {
        return role;
    }
    //Constructor:
    public User(String user_name, long ID, String email, String password, String role) {
        this.user_name = user_name;
        this.ID = ID;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    //abstract method:
    public abstract void set_role();

    //Checking password (băm SHA-256 trước khi so sánh với mật khẩu DB):
    public boolean check_password(String input_password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input_password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String calculatedHash = hexString.toString();

            // ── THÊM ĐOẠN LOG ĐỂ KIỂM TRA LỆCH Ở ĐÂU ──────────────────────────
            System.out.println("\n====== [LOGIN] ======");
            System.out.println("ACCOUNT: " + get_user_name());
            System.out.println("INPUT PASSWORD: [" + input_password + "]");
            System.out.println("CALCULATED HASH:   [" + calculatedHash + "]");
            System.out.println("ACTUAL HASH IN DB: [" + password + "]");
            System.out.println("MATCH RESULT:    " + calculatedHash.equals(password));
            System.out.println("================================\n");
            // ─────────────────────────────────────────────────────────────────

            return calculatedHash.equals(password);
        } catch (java.security.NoSuchAlgorithmException e) {
            return false;
        }
    }
    
    //Getters and Setters:
    public String get_user_name() {
        return user_name;
    }

    public long get_ID() {
        return ID;
    }

    public String get_email() {
        return email;
    }

    public String get_password() {
        return password;
    }

    
    public void set_user_name( String new_user_name ) {
        this.user_name = new_user_name;
    }

    public void set_ID( long new_ID ) {
        this.ID = new_ID;
    }

    public void set_email( String new_email ) {
        this.email = new_email;
    }    
    public void set_password( String new_password ) {
        this.password = new_password;
    }

}
