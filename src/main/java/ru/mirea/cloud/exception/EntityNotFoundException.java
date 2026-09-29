package ru.mirea.cloud.exception;

public class EntityNotFoundException extends BusinessException {
    public EntityNotFoundException(String entity, long id) {
        super(entity + " с ID " + id + " не найден(а).");
    }
}
