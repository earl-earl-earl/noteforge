package com.mycompany.notesmanagement.models;

import com.mycompany.notesmanagement.dialogs.DatabaseError;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Database {

    private String path;

    public Database() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("properties/database.properties")) {
            Properties prop = new Properties();
            if (input != null) {
                prop.load(input);
                this.path = prop.getProperty("db.path");
            } else {
                System.err.println("database.properties not found!");
                new DatabaseError().setVisible(true);
            }
        } catch (IOException ex) {
            System.err.println("Error loading database.properties: " + ex.getMessage());
            new DatabaseError().setVisible(true);
        }
    }


    public String getPath() {
        return path;
    }
}