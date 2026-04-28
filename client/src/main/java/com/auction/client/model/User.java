package com.auction.client.model;

public abstract class User {
    
    //Call Fields:
    private String user_name;
    private String ID;
    private String email;
    private String password;
    protected String role;

    //Constructor:
    public User(String user_name, String ID, String email, String password, String role) {
        this.user_name = user_name;
        this.ID = ID;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    //abstract method:
    public abstract String set_role();

    //Checking password:
    public boolean check_password(String input_password) {
        return input_password.equals(password);
    }
    
    //Getters and Setters:
    public String get_user_name() {
        return user_name;
    }

    public String get_ID() {
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

    public void set_ID( String new_ID ) {
        this.ID = new_ID;
    }

    public void set_email( String new_email ) {
        this.email = new_email;
    }    
}
