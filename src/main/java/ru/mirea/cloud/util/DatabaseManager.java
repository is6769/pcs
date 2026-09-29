package ru.mirea.cloud.util;

import ru.mirea.cloud.exception.DataAccessException;
import java.sql.*;
import java.util.*;

public final class DatabaseManager {
    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static DatabaseManager fromEnvironment() {
        return new DatabaseManager(env("DB_URL", "jdbc:postgresql://localhost:5433/cloud_provider"),
                env("DB_USER", "cloud"), env("DB_PASSWORD", "cloud_dev_password"));
    }

    private static String env(String key, String fallback) {
        return System.getenv().getOrDefault(key, fallback);
    }

    public Connection open() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "15");
        return DriverManager.getConnection(url, properties);
    }

    public List<String> listTables() {
        List<String> tables = new ArrayList<>();
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = ? ORDER BY table_name")) {
            statement.setString(1, "public");
            statement.setString(2, "BASE TABLE");
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) tables.add(rows.getString(1));
            }
            return tables;
        } catch (SQLException e) { throw new DataAccessException(e); }
    }
}
