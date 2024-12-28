package com.mycompany.notesmanagement.controllers;

public class UserSession {
    private static UserSession instance;
    private int userId; // Add the userId field
    private String loginInput;
    private boolean loggedIn;

    private UserSession() {
        // Private constructor to prevent instantiation
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void logout() {
        this.setLoginInput(null);
        this.setUserId(0); // Clear userId on logout
        this.loggedIn = false;
    }


    public String getLoginInput() {
        return loginInput;
    }

    public void setLoginInput(String loginInput) {
        this.loginInput = loginInput;
    }

    // Getter and setter for userId
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}