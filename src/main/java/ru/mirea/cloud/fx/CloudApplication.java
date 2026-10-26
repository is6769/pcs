package ru.mirea.cloud.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class CloudApplication extends Application {
    private AppContext context;

    @Override public void start(Stage stage) {
        try {
            context = AppContext.create();
        } catch (IllegalStateException error) {
            FxAlerts.error(null, "Проверьте настройки подключения",
                    new ru.mirea.cloud.exception.BusinessException(error.getMessage()));
            Platform.exit();
            return;
        }
        try {
            showMainWindow(stage);
        } catch (Exception error) {
            System.getLogger(CloudApplication.class.getName()).log(System.Logger.Level.ERROR,
                    "Не удалось загрузить интерфейс", error);
            FxAlerts.error(null, "Не удалось открыть приложение",
                    new ru.mirea.cloud.exception.BusinessException("Проверьте наличие FXML и CSS в ресурсах и пересоберите проект."));
            context.close();
            Platform.exit();
        }
    }

    private void showMainWindow(Stage stage) throws java.io.IOException {
        FXMLLoader loader = new FXMLLoader(CloudApplication.class.getResource("main.fxml"));
        loader.setControllerFactory(type -> {
            if (type == MainController.class) return new MainController(context);
            throw new IllegalArgumentException("Unknown controller: " + type.getName());
        });
        Scene scene = new Scene(loader.load(), 1320, 820);
        scene.getStylesheets().add(CloudApplication.class.getResource("app.css").toExternalForm());
        stage.setTitle("S-Cloud — управление облачными ресурсами");
        stage.setMinWidth(1060);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
        ((MainController) loader.getController()).refresh();
    }

    @Override public void stop() { if (context != null) context.close(); }

    public static void main(String[] args) { launch(args); }
}
