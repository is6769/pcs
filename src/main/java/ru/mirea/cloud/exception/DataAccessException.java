package ru.mirea.cloud.exception;

import java.sql.SQLException;

public class DataAccessException extends RuntimeException {
    public DataAccessException(SQLException cause) {
        super(message(cause.getSQLState()), cause);
    }

    private static String message(String state) {
        if (state == null || state.startsWith("08"))
            return "Нет подключения к PostgreSQL. Проверьте Docker, DB_URL, DB_USER и DB_PASSWORD.";
        return switch (state) {
            case "23505" -> "Такая запись уже существует (email должен быть уникальным).";
            case "23503" -> "Связанная запись отсутствует или используется другими записями.";
            case "23514", "23502", "22001" -> "Данные нарушают ограничения базы данных.";
            case "28P01", "28000" -> "PostgreSQL отклонил учетные данные. Проверьте DB_USER и DB_PASSWORD.";
            default -> "Ошибка SQL (SQLState=" + state + "). Проверьте схему базы данных.";
        };
    }
}
