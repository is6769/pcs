package ru.mirea.cloud.fx;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.cloud.model.Client;

public final class ClientEditorController {
    @FXML private Parent formRoot;
    @FXML private Label heading;
    @FXML private Label errorLabel;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField companyField;

    private Stage stage;
    private AppContext context;
    private Client original;
    private Runnable onSaved;

    @FXML private void initialize() {
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
    }

    public void configure(Stage stage, AppContext context, Client client, Runnable onSaved) {
        this.stage = stage;
        this.context = context;
        this.original = client;
        this.onSaved = onSaved;
        heading.setText(client == null ? "Новый клиент" : "Редактирование клиента");
        if (client != null) {
            nameField.setText(client.getName());
            emailField.setText(client.getEmail());
            companyField.setText(client.getCompany());
        }
    }

    @FXML private void save() {
        String name = nameField.getText();
        String email = emailField.getText();
        String company = companyField.getText();
        if (name.isBlank()) { invalid(nameField, "Имя: обязательное поле."); return; }
        if (email.isBlank()) { invalid(emailField, "Email: обязательное поле."); return; }
        if (company.isBlank()) { invalid(companyField, "Компания: обязательное поле."); return; }
        EditorDialogs.save(stage, formRoot, errorLabel, context.tasks(), () -> {
            if (original == null) context.clients().create(name, email, company);
            else context.clients().update(original.getId(), name, email, company);
            return null;
        }, onSaved);
    }

    private void invalid(TextField field, String message) {
        errorLabel.setText(message);
        field.requestFocus();
    }

    @FXML private void cancel() { stage.close(); }
}
