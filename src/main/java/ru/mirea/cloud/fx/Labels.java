package ru.mirea.cloud.fx;

import javafx.util.StringConverter;
import ru.mirea.cloud.model.*;

import java.util.function.Function;

/** User-facing names shared by tables, filters and editor forms. */
public final class Labels {
    private Labels() { }

    public static String status(RequestStatus status) {
        if (status == null) return "Все статусы";
        return switch (status) {
            case NEW -> "Новая";
            case APPROVED -> "Одобрена";
            case PROVISIONED -> "Ресурс выделен";
            case REJECTED -> "Отклонена";
            case CANCELLED -> "Отменена";
        };
    }

    public static String type(ResourceType type) {
        if (type == null) return "Все типы";
        return switch (type) {
            case VIRTUAL_MACHINE -> "Виртуальная машина";
            case DATABASE -> "Управляемая БД";
            case OBJECT_STORAGE -> "Объектное хранилище";
        };
    }

    public static String region(Region region) {
        if (region == null) return "Все регионы";
        return switch (region) {
            case RU_MOSCOW -> "Москва";
            case RU_SPB -> "Санкт-Петербург";
            case RU_KAZAN -> "Казань";
        };
    }

    public static String client(Client client) {
        return client == null ? "" : client.getName() + " · " + client.getCompany() + " · " + client.getEmail();
    }

    public static <T> StringConverter<T> converter(Function<T, String> label) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return label.apply(value); }
            @Override public T fromString(String text) {
                throw new UnsupportedOperationException("Выберите значение из списка.");
            }
        };
    }
}
