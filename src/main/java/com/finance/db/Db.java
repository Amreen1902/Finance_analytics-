package com.finance.db;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db {
    private static volatile String url = "jdbc:sqlite:finance.db";

    private Db() {}

    public static void setUrl(String newUrl) {
        url = newUrl;
    }

    public static void resetUrl() {
        url = "jdbc:sqlite:finance.db";
    }

    /** New connection per unit of work (SQLite is file-based, so this is cheap). */
    public static Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            s.execute("PRAGMA busy_timeout = 5000");
        }
        return c;
    }

    /** Runs schema.sql (tables, indexes, views). Safe to call on every start. */
    public static void init() {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            String ddl = new String(Db.class.getResourceAsStream("/schema.sql").readAllBytes(),
                    StandardCharsets.UTF_8);
            for (String stmt : ddl.split(";\\s*\\R")) {
                if (!stmt.isBlank()) s.execute(stmt);
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Database init failed: " + e.getMessage(), e);
        }
    }
}
