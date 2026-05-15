package com.auction.server.service;

import com.auction.server.dao.UserDao;
import com.auction.server.model.User;

import java.util.List;
import java.util.Optional;


public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    
    public User getById(long id) {
        return userDao.findById(id).orElseThrow(() -> new IllegalArgumentException("User does not exist, id = " + id));
    }

    
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username does not exist");
        return userDao.findByUsername(username);
    }


    public List<User> getAllUsers() {
        return userDao.findAll();
    }

 
    public User updateEmail(long userId, String newEmail) {
        if (newEmail == null || !newEmail.contains("@"))
            throw new IllegalArgumentException("Email is invalid: " + newEmail);

        User user = getById(userId);
        user.set_email(newEmail);
        return userDao.update(user);
    }

    public User updatePassword(long userId, String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isBlank())
            throw new IllegalArgumentException("Password is invalid");

        User user = getById(userId);
        user.set_password(hashedPassword);
        return userDao.update(user);
    }


    public boolean deleteUser(long userId) {
        boolean deleted = userDao.deleteById(userId);
        if (!deleted)
            System.out.println("UserService: User not found for deletion, id = " + userId);
        return deleted;
    }
    
}
