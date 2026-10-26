package ru.mirea.cloud.fx;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import ru.mirea.cloud.exception.BusinessException;
import ru.mirea.cloud.exception.DataAccessException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;

public final class FxAlerts {
    private FxAlerts() { }

    public static void error(Window owner, String title, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message(error), ButtonType.OK);
        configure(alert, owner, title);
        alert.showAndWait();
    }

    public static boolean confirm(Window owner, String title, String message) {
        ButtonType delete = new ButtonType("Удалить", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, delete, cancel);
        configure(alert, owner, title);
        ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(delete)).setDefaultButton(false);
        ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        alert.setOnShown(event -> alert.getDialogPane().lookupButton(cancel).requestFocus());
        return alert.showAndWait().orElse(cancel) == delete;
    }

    public static String message(Throwable error) {
        if (error instanceof BusinessException || error instanceof DataAccessException) return error.getMessage();
        if (error instanceof FileAlreadyExistsException) return "Файл уже существует. Выберите другое имя для экспорта.";
        if (error instanceof AccessDeniedException) return "Нет доступа к файлу. Выберите другую папку или закройте файл в Excel.";
        if (error instanceof IOException) return "Не удалось прочитать или записать файл. Проверьте выбранную папку и права доступа.";
        return "Не удалось выполнить операцию. Обновите данные и повторите попытку.";
    }

    private static void configure(Alert alert, Window owner, String title) {
        if (owner != null) alert.initOwner(owner);
        alert.setTitle("S-Cloud");
        alert.setHeaderText(title);
        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(440);
        var css = FxAlerts.class.getResource("app.css");
        if (css != null) alert.getDialogPane().getStylesheets().add(css.toExternalForm());
    }
}
