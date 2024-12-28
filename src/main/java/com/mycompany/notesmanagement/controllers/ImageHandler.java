/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.notesmanagement.controllers;

/**
 *
 * @author Regine Torremoro
 */
import com.mycompany.notesmanagement.dialogs.DatabaseError;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;

public class ImageHandler {

    private final DatabaseConnector database = new DatabaseConnector();

    public byte[] retrieveImage(int input) throws SQLException, IOException {
        byte[] imageData = null;

        try {
            database.connect();

            String sql = "SELECT user_profile_picture FROM users WHERE user_id = ?";
            try (PreparedStatement stmt = database.getConnection().prepareStatement(sql)) {
                stmt.setInt(1, input);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Blob blob = rs.getBlob("user_profile_picture");
                        if (blob != null) {
                            imageData = blob.getBytes(1, (int) blob.length());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            
        } finally {
            try {
                if (database != null) {
                    database.close();
                }
            } catch (SQLException e) {
            }
        }

        return imageData;
    }

    public void saveImageToFile(byte[] imageData, String filename) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(filename)) {
            outputStream.write(imageData);
        }
    }
}
