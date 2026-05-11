package com.auction.server.model;

public class Admin extends User {
    
    //Constructor:
    public Admin(String name, long ID, String email, String password, String role) {
        super(name, ID, email, password, role);
    }

    @Override
    public void set_role() {
        this.role = "Admin";
    }

}
