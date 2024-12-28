package com.mycompany.notesmanagement.controllers;
import com.mycompany.notesmanagement.models.Database;
import java.sql.*;

public class DatabaseConnector {

    private Connection connection;
    private final Database database = new Database();

    public Connection connect() throws SQLException {
        if (getConnection() == null || getConnection().isClosed()) {
            try {
                String url = "jdbc:ucanaccess://" + database.getPath();
                System.out.println("Connecting to: " + url);
                connection = DriverManager.getConnection(url);
            } catch (SQLException ex) {
                System.err.println("Database connection error: " + ex.getMessage());
                throw new SQLException("Failed to connect to the database: " + ex.getMessage(), ex);
            }
        }
        return getConnection();
    }

    public void close() throws SQLException {
        if (getConnection() != null && !getConnection().isClosed()) {
            getConnection().close();
        }
    }

    public boolean isConnected() throws SQLException {
        return getConnection() != null && !getConnection().isClosed();
    }

    public Connection getConnection() {
        return connection;
    }
}