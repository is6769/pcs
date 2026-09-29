package ru.mirea.cloud.service;

import ru.mirea.cloud.exception.BusinessException;

final class Validation {
    private Validation() { }
    static String required(String value, String field, int max) {
        if (value == null || value.isBlank()) throw new BusinessException(field + ": обязательное поле.");
        String trimmed = value.strip();
        if (trimmed.length() > max) throw new BusinessException(field + ": максимум " + max + " символов.");
        return trimmed;
    }
    static void positiveId(long id) {
        if (id <= 0) throw new BusinessException("ID должен быть положительным.");
    }
}
