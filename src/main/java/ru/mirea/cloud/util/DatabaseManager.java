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
        Properties settings = connectionSettings(System.getProperties(), System.getenv());
        return new DatabaseManager(settings.getProperty("db.url"), settings.getProperty("db.user"),
                settings.getProperty("db.password"));
    }

    /** All settings are required; Java system properties override environment variables. */
    static Properties connectionSettings(Properties system, Map<String, String> environment) {
        Properties settings = new Properties();
        for (String key : List.of("url", "user", "password")) {
            String property = "db." + key;
            String value = system.getProperty(property);
            if (value == null) value = environment.get("DB_" + key.toUpperCase(Locale.ROOT));
            if (value == null || value.isBlank())
                throw new IllegalStateException("Не задан параметр " + property + ". Укажите системное свойство "
                        + property + " или переменную окружения DB_" + key.toUpperCase(Locale.ROOT) + ".");
            settings.setProperty(property, key.equals("password") ? value : value.strip());
        }
        if (!settings.getProperty("db.url").startsWith("jdbc:postgresql:"))
            throw new IllegalStateException("db.url должен быть JDBC-адресом PostgreSQL (jdbc:postgresql://сервер:порт/база).");
        return settings;
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
