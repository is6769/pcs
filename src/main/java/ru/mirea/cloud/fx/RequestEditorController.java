package ru.mirea.cloud.fx;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ru.mirea.cloud.exception.BusinessException;
import ru.mirea.cloud.model.*;

import java.util.List;

public final class RequestEditorController {
    @FXML private Parent formRoot;
    @FXML private Label heading;
    @FXML private Label errorLabel;
    @FXML private ComboBox<Client> clientBox;
    @FXML private ComboBox<ResourceType> typeBox;
    @FXML private ComboBox<Region> regionBox;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private TextField cpuField;
    @FXML private TextField ramField;
    @FXML private TextField storageField;
    @FXML private Button saveButton;

    private Stage stage;
    private AppContext context;
    private ResourceRequest original;
    private Runnable onSaved;
    private String computeCpu = "2";
    private String computeRam = "4";

    @FXML private void initialize() {
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        clientBox.setConverter(Labels.converter(Labels::client));
        typeBox.setConverter(Labels.converter(Labels::type));
        regionBox.setConverter(Labels.converter(Labels::region));
        typeBox.getItems().setAll(ResourceType.values());
        regionBox.getItems().setAll(Region.values());
        cpuField.setText(computeCpu);
        ramField.setText(computeRam);
        storageField.setText("40");
        typeBox.valueProperty().addListener((observable, previous, current) -> {
            boolean storage = current == ResourceType.OBJECT_STORAGE;
            if (storage) {
                computeCpu = cpuField.getText();
                computeRam = ramField.getText();
                cpuField.setText("0");
                ramField.setText("0");
            } else if (previous == ResourceType.OBJECT_STORAGE) {
                cpuField.setText(computeCpu);
                ramField.setText(computeRam);
            }
            cpuField.setDisable(storage);
            ramField.setDisable(storage);
        });
    }

    public void configure(Stage stage, AppContext context, ResourceRequest request,
                          List<Client> clients, Runnable onSaved) {
        this.stage = stage;
        this.context = context;
        this.original = request;
        this.onSaved = onSaved;
        heading.setText(request == null ? "Новая заявка" : "Редактирование заявки");
        clientBox.getItems().setAll(clients);
        if (request == null) {
            typeBox.setValue(ResourceType.VIRTUAL_MACHINE);
            regionBox.setValue(Region.RU_MOSCOW);
            if (clients.size() == 1) clientBox.setValue(clients.getFirst());
        } else {
            clientBox.setValue(clients.stream().filter(c -> c.getId() == request.getClientId()).findFirst().orElse(null));
            titleField.setText(request.getTitle());
            descriptionField.setText(request.getDescription());
            typeBox.setValue(request.getResourceType());
            regionBox.setValue(request.getRegion());
            cpuField.setText(Integer.toString(request.getCpu()));
            ramField.setText(Integer.toString(request.getRamGb()));
            storageField.setText(Integer.toString(request.getStorageGb()));
        }
        if (clients.isEmpty()) {
            errorLabel.setText("Сначала добавьте клиента на вкладке «Клиенты».");
            saveButton.setDisable(true);
        } else if (request != null && request.getStatus() != RequestStatus.NEW) {
            errorLabel.setText("Изменять параметры можно только у новой заявки.");
            saveButton.setDisable(true);
        }
    }

    @FXML private void save() {
        Client client = clientBox.getValue();
        String title = titleField.getText();
        String description = descriptionField.getText();
        ResourceType type = typeBox.getValue();
        Region region = regionBox.getValue();
        if (client == null) { invalid(clientBox, "Выберите клиента из списка."); return; }
        if (title.isBlank()) { invalid(titleField, "Название: обязательное поле."); return; }
        if (type == null) { invalid(typeBox, "Выберите тип ресурса."); return; }
        if (region == null) { invalid(regionBox, "Выберите регион."); return; }
        final int cpu;
        final int ram;
        final int storage;
        try {
            cpu = number(cpuField, "CPU");
            ram = number(ramField, "RAM");
            storage = number(storageField, "Диск");
        } catch (BusinessException error) {
            errorLabel.setText(error.getMessage());
            return;
        }
        EditorDialogs.save(stage, formRoot, errorLabel, context.tasks(), () -> {
            if (original == null) {
                context.requests().create(client.getId(), title, description, type, region, cpu, ram, storage);
            } else {
                context.requests().update(original.getId(), client.getId(), title, description,
                        type, region, cpu, ram, storage, original.getVersion());
            }
            return null;
        }, onSaved);
    }

    private int number(TextField field, String name) {
        try {
            return Integer.parseInt(field.getText().strip());
        } catch (NumberFormatException error) {
            field.requestFocus();
            throw new BusinessException(name + ": введите целое число без дробной части.");
        }
    }

    private void invalid(Control control, String message) {
        errorLabel.setText(message);
        control.requestFocus();
    }

    @FXML private void cancel() { stage.close(); }
}
