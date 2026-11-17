package com.example.warehouse.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final Path DIR = Paths.get(System.getProperty("user.home"), ".warehouse-erp");

    private Database() {
    }

    public static Connection connect() throws SQLException {
        try {
            Files.createDirectories(DIR);
        } catch (IOException e) {
            throw new SQLException("Cannot create data directory " + DIR, e);
        }
        return DriverManager.getConnection("jdbc:sqlite:" + DIR.resolve("warehouse.db"));
    }

    public static void init() {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS products (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        sku TEXT NOT NULL UNIQUE,
                        name TEXT NOT NULL,
                        location TEXT,
                        quantity INTEGER NOT NULL DEFAULT 0,
                        min_stock INTEGER NOT NULL DEFAULT 0,
                        price REAL NOT NULL DEFAULT 0
                    )""");
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS movements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        sku TEXT NOT NULL,
                        delta INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        note TEXT,
                        created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
                    )""");
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS label_templates (
                        name TEXT PRIMARY KEY,
                        data TEXT NOT NULL
                    )""");
        } catch (SQLException e) {
            throw new DataAccessException("Could not initialise database: " + e.getMessage(), e);
        }
    }
}
