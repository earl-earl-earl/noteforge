/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.notesmanagement.models;

/**
 *
 * @author Regine Torremoro
 */
import com.mycompany.notesmanagement.controllers.DatabaseConnector;
import com.mycompany.notesmanagement.dialogs.DatabaseError;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CurrentUser {

    private final DatabaseConnector database = new DatabaseConnector();
    private int id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String userName;
    private String gender;
    private Blob profilePicture;

    public CurrentUser(int userId) {

        PreparedStatement statement = null;
        ResultSet resultSet = null;

        String query = "SELECT * FROM users WHERE user_id = ?";

        try {

            database.connect();
            statement = database.getConnection().prepareStatement(query);
            statement.setInt(1, userId);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                setId(resultSet.getInt("user_id"));
                setEmail(resultSet.getString("user_email"));
                setPassword(resultSet.getString("user_password"));
                setFirstName(resultSet.getString("user_firstname"));
                setLastName(resultSet.getString("user_lastname"));
                setUserName(resultSet.getString("user_username"));
                setGender(resultSet.getString("user_gender"));
                setProfilePicture(resultSet.getBlob("user_profile_picture"));
            }
            

        } catch (SQLException e) {
            new DatabaseError().setVisible(true);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                database.close();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
            }
        }

    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public final void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public final void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * @param firstName the firstName to set
     */
    public final void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * @param lastName the lastName to set
     */
    public final void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public final void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the gender
     */
    public String getGender() {
        return gender;
    }

    /**
     * @param gender the gender to set
     */
    public final void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * @return the profilePicture
     */
    public Blob getProfilePicture() {
        return profilePicture;
    }

    /**
     * @param profilePicture the profilePicture to set
     */
    public void setProfilePicture(Blob profilePicture) {
        this.profilePicture = profilePicture;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

}
