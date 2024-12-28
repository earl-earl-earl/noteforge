/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.notesmanagement.controllers;

import com.mycompany.notesmanagement.models.Database;
import com.mycompany.notesmanagement.models.User;
import com.mycompany.notesmanagement.dialogs.DatabaseError;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserCreation {

    private final User userDetails;
    private int id;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final String username;
    private final String gender;

    public UserCreation(User userDetails) {
        this.userDetails = userDetails;
        this.id = this.userDetails.getId();
        this.email = this.userDetails.getEmail();
        this.password = this.userDetails.getPassword();
        this.firstName = this.userDetails.getFirstName();
        this.lastName = this.userDetails.getLastName();
        this.username = this.userDetails.getUsername();
        this.gender = this.userDetails.getGender();
    }

    public void insertValuesToDatabase() {

        DatabaseConnector connector = new DatabaseConnector();

        try {
            connector.connect();
            if (connector.isConnected()) {
                PreparedStatement statement = null;
                FileInputStream inputStream = null;

                String query = "INSERT INTO users (user_email, user_firstname, user_lastname, user_username, user_password, user_gender) VALUES (?, ?, ?, ?, ?, ?)";

                try {

                    if (email == null || email.isEmpty()
                            || firstName == null || firstName.isEmpty()
                            || lastName == null || lastName.isEmpty()
                            || username == null || username.isEmpty()
                            || password == null || password.isEmpty()
                            || gender == null || gender.isEmpty()) {

                        throw new IllegalArgumentException("Invalid input values");
                    }

                    statement = connector.getConnection().prepareStatement(query);

                    statement.setString(1, email);
                    statement.setString(2, firstName);
                    statement.setString(3, lastName);
                    statement.setString(4, username);
                    statement.setString(5, password);
                    statement.setString(6, gender);

                    // **Crucial:** Use setBytes() or setBinaryStream():
//                    if (profilePicture.length() <= Integer.MAX_VALUE) {
//                        byte[] imageBytes = new byte[(int) profilePicture.length()];  // If file is small enough
//                        inputStream.read(imageBytes);
//                        statement.setBytes(7, imageBytes);
//                        inputStream.close();
//                        inputStream = null; // Close after reading
//                    } else {
//                        statement.setBinaryStream(7, inputStream, (int) profilePicture.length()); // If it might be very large
//                        // Input stream will be closed in the finally block.
//                    }
                    statement.executeUpdate();

                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int generatedId = generatedKeys.getInt(1);
                            id = generatedId;
                            userDetails.setId(generatedId);
                            System.out.println("Generated ID: " + generatedId);
                        } else {
                            throw new SQLException("Creating user failed, no ID obtained.");
                        }
                    }

                } catch (SQLException e) {
                    Logger.getLogger(UserCreation.class.getName()).log(Level.SEVERE, "SQL error: " + e.getMessage(), e);
                    new DatabaseError().setVisible(true);
                } catch (IllegalArgumentException e) {
                    Logger.getLogger(UserCreation.class.getName()).log(Level.WARNING, "Validation error: " + e.getMessage(), e);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Logger.getLogger(UserCreation.class.getName()).log(Level.SEVERE, "Error closing FileInputStream: " + e.getMessage(), e);
                        }
                    }
                    if (statement != null) {
                        try {
                            statement.close();
                        } catch (SQLException e) {
                            Logger.getLogger(UserCreation.class.getName()).log(Level.SEVERE, "Error closing statement: " + e.getMessage(), e);
                        }
                    }
                }
            } else {
                Logger.getLogger(UserCreation.class.getName()).log(Level.SEVERE, "Failed to connect to the database");

            }
        } catch (SQLException e) {
            Logger.getLogger(UserCreation.class.getName()).log(Level.SEVERE, "SQL error during connection: " + e.getMessage(), e);

        } finally {
            try {
                connector.close();
            } catch (SQLException e) {
                Logger.getLogger(UserCreation.class.getName()).log(Level.SEVERE, "Error closing connection: " + e.getMessage(), e);

            }
        }

    }

}
