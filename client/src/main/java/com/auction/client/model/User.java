package com.auction.client.model;

public abstract class User {
    
    //Call Fields:
    String user_name;
    String ID;
    String email;
    private String password;
    String role;

    //Constructor:
    public User(String user_name, String ID, String email, String password, String role) {
        this.user_name = user_name;
        this.ID = ID;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    //abstract method:
    public abstract String get_role();

    //Checking password:
    public boolean check_password(String input_password) {
        return input_password.equals(password);
    }
    

}
