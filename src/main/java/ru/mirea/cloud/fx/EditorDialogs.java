package ru.mirea.cloud.fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.mirea.cloud.model.Client;
import ru.mirea.cloud.model.ResourceRequest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/** Loads the FXML forms and shares their asynchronous save/error lifecycle. */
public final class EditorDialogs {
    private EditorDialogs() { }

    public static void showRequest(Window owner, AppContext context, ResourceRequest request,
                                   List<Client> clients, Runnable onSaved) {
        try {
            FXMLLoader loader = new FXMLLoader(EditorDialogs.class.getResource("request-editor.fxml"));
            Parent form = loader.load();
            Stage stage = stage(owner, request == null ? "Новая заявка" : "Редактирование заявки", form, 680, 680);
            RequestEditorController controller = loader.getController();
            controller.configure(stage, context, request, List.copyOf(clients), onSaved);
            stage.showAndWait();
        } catch (IOException e) {
            FxAlerts.error(owner, "Не удалось открыть форму заявки", e);
        }
    }

    public static void showClient(Window owner, AppContext context, Client client, Runnable onSaved) {
        try {
            FXMLLoader loader = new FXMLLoader(EditorDialogs.class.getResource("client-editor.fxml"));
            Parent form = loader.load();
            Stage stage = stage(owner, client == null ? "Новый клиент" : "Редактирование клиента", form, 560, 440);
            ClientEditorController controller = loader.getController();
            controller.configure(stage, context, client, onSaved);
            stage.showAndWait();
        } catch (IOException e) {
            FxAlerts.error(owner, "Не удалось открыть форму клиента", e);
        }
    }

    private static Stage stage(Window owner, String title, Parent form, double width, double height) {
        Stage stage = new Stage();
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title + " — S-Cloud");
        Scene scene = new Scene(form, width, height);
        scene.getStylesheets().add(EditorDialogs.class.getResource("app.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(520);
        stage.setMinHeight(400);
        // A save cannot be abandoned while the database operation is in progress.
        stage.setOnCloseRequest(event -> {
            if (form.isDisabled()) event.consume();
        });
        return stage;
    }

    static void save(Stage stage, Parent form, Label error, FxTasks tasks,
                     Callable<Void> action, Runnable onSaved) {
        error.setText("");
        form.setDisable(true);
        tasks.submit(action, ignored -> {
            form.setDisable(false);
            stage.close();
            onSaved.run();
        }, failure -> {
            form.setDisable(false);
            error.setText(FxAlerts.message(failure));
        });
    }
}
