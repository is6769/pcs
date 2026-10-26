package ru.mirea.cloud.fx;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;
import ru.mirea.cloud.model.*;
import ru.mirea.cloud.service.StatisticsService;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/** Coordinates views; persistence and domain validation remain in services. */
public final class MainController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final AppContext context;
    private final ObservableList<ResourceRequest> requests = FXCollections.observableArrayList();
    private final ObservableList<Client> clients = FXCollections.observableArrayList();
    private final FilteredList<ResourceRequest> filteredRequests = new FilteredList<>(requests);
    private final FilteredList<Client> filteredClients = new FilteredList<>(clients);
    private final Map<Long, Client> clientsById = new HashMap<>();
    private SortedList<ResourceRequest> sortedRequests;
    private boolean loaded;

    @FXML private TabPane tabs;
    @FXML private Tab requestsTab;
    @FXML private Tab clientsTab;
    @FXML private TableView<ResourceRequest> requestTable;
    @FXML private TableColumn<ResourceRequest, String> titleColumn;
    @FXML private TableColumn<ResourceRequest, String> requestClientColumn;
    @FXML private TableColumn<ResourceRequest, String> typeColumn;
    @FXML private TableColumn<ResourceRequest, String> regionColumn;
    @FXML private TableColumn<ResourceRequest, RequestStatus> statusColumn;
    @FXML private TableColumn<ResourceRequest, Integer> cpuColumn;
    @FXML private TableColumn<ResourceRequest, Integer> ramColumn;
    @FXML private TableColumn<ResourceRequest, Integer> storageColumn;
    @FXML private TableColumn<ResourceRequest, LocalDateTime> createdColumn;
    @FXML private TextField requestSearch;
    @FXML private ComboBox<RequestStatus> statusFilter;
    @FXML private ComboBox<ResourceType> typeFilter;
    @FXML private ComboBox<Region> regionFilter;
    @FXML private ComboBox<Client> clientFilter;
    @FXML private Label requestCount;
    @FXML private Label detailTitle;
    @FXML private Label detailClient;
    @FXML private Label detailDescription;
    @FXML private Button requestAdd;
    @FXML private Button requestEdit;
    @FXML private Button requestDelete;
    @FXML private Button requestStatus;
    @FXML private Button viewClient;
    @FXML private Button exportButton;
    @FXML private Button refreshButton;
    @FXML private TableView<Client> clientTable;
    @FXML private TableColumn<Client, String> clientNameColumn;
    @FXML private TableColumn<Client, String> clientCompanyColumn;
    @FXML private TableColumn<Client, String> clientEmailColumn;
    @FXML private TableColumn<Client, Long> clientRequestsColumn;
    @FXML private TextField clientSearch;
    @FXML private Label clientCount;
    @FXML private Button clientAdd;
    @FXML private Button clientEdit;
    @FXML private Button clientDelete;
    @FXML private Button viewRequests;
    @FXML private Label clientHint;
    @FXML private FlowPane statisticsCards;
    @FXML private Label statisticsHint;
    @FXML private Label operationStatus;
    @FXML private ProgressIndicator progress;

    public MainController(AppContext context) {
        this.context = context;
    }

    @FXML private void initialize() {
        configureRequestTable();
        configureClientTable();
        configureFilter(statusFilter, Arrays.asList(RequestStatus.values()), Labels::status, "Все статусы");
        configureFilter(typeFilter, Arrays.asList(ResourceType.values()), Labels::type, "Все типы");
        configureFilter(regionFilter, Arrays.asList(Region.values()), Labels::region, "Все регионы");
        configureFilter(clientFilter, List.of(), Labels::client, "Все клиенты");
        requestSearch.textProperty().addListener((observable, oldValue, value) -> applyFilters());
        clientSearch.textProperty().addListener((observable, oldValue, value) -> applyClientSearch());
        requestTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, value) -> {
            showDetails(value);
            updateActions();
        });
        clientTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, value) -> updateActions());
        progress.visibleProperty().bind(context.tasks().busyProperty());
        progress.managedProperty().bind(progress.visibleProperty());
        context.tasks().busyProperty().addListener((observable, oldValue, value) -> updateActions());
        refreshButton.disableProperty().bind(context.tasks().busyProperty());
        showDetails(null);
        updateActions();
    }

    private void configureRequestTable() {
        column(titleColumn, ResourceRequest::getTitle);
        column(requestClientColumn, request -> clientName(request.getClientId()));
        column(typeColumn, request -> Labels.type(request.getResourceType()));
        column(regionColumn, request -> Labels.region(request.getRegion()));
        column(statusColumn, ResourceRequest::getStatus);
        column(cpuColumn, ResourceRequest::getCpu);
        column(ramColumn, ResourceRequest::getRamGb);
        column(storageColumn, ResourceRequest::getStorageGb);
        column(createdColumn, ResourceRequest::getCreatedAt);
        titleColumn.setComparator(String.CASE_INSENSITIVE_ORDER);
        requestClientColumn.setComparator(String.CASE_INSENSITIVE_ORDER);
        statusColumn.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(RequestStatus value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                if (empty || value == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(Labels.status(value));
                    badge.getStyleClass().addAll("status-badge", "status-" + value.name().toLowerCase(Locale.ROOT));
                    setGraphic(badge);
                }
            }
        });
        createdColumn.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : DATE.format(value));
            }
        });
        sortedRequests = new SortedList<>(filteredRequests);
        sortedRequests.comparatorProperty().bind(requestTable.comparatorProperty());
        requestTable.setItems(sortedRequests);
        createdColumn.setSortType(TableColumn.SortType.DESCENDING);
        requestTable.getSortOrder().add(createdColumn);
        requestTable.setPlaceholder(new Label("Заявок нет. Добавьте первую заявку или измените фильтры."));
        requestTable.setRowFactory(table -> {
            TableRow<ResourceRequest> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()
                        && row.getItem().getStatus() == RequestStatus.NEW && !context.tasks().isBusy()) {
                    editRequest();
                }
            });
            return row;
        });
    }

    private void configureClientTable() {
        column(clientNameColumn, Client::getName);
        column(clientCompanyColumn, Client::getCompany);
        column(clientEmailColumn, Client::getEmail);
        column(clientRequestsColumn, client -> requests.stream().filter(r -> r.getClientId() == client.getId()).count());
        clientNameColumn.setComparator(String.CASE_INSENSITIVE_ORDER);
        clientCompanyColumn.setComparator(String.CASE_INSENSITIVE_ORDER);
        SortedList<Client> sorted = new SortedList<>(filteredClients);
        sorted.comparatorProperty().bind(clientTable.comparatorProperty());
        clientTable.setItems(sorted);
        clientTable.getSortOrder().add(clientNameColumn);
        clientTable.setPlaceholder(new Label("Клиенты не найдены. Добавьте клиента или измените поиск."));
        clientTable.setRowFactory(table -> {
            TableRow<Client> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty() && !context.tasks().isBusy()) editClient();
            });
            return row;
        });
    }

    private static <T, V> void column(TableColumn<T, V> column, Function<T, V> value) {
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
    }

    private <T> void configureFilter(ComboBox<T> box, List<T> values, Function<T, String> label, String allLabel) {
        box.getItems().add(null);
        box.getItems().addAll(values);
        box.setConverter(new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? allLabel : label.apply(value); }
            @Override public T fromString(String value) { throw new UnsupportedOperationException("Выберите значение из списка"); }
        });
        // An explicit null item represents the unrestricted filter.
        box.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(T value, boolean empty) {
                super.updateItem(value, empty);
                setText(value == null ? allLabel : label.apply(value));
            }
        });
        box.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(T value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value == null ? allLabel : label.apply(value));
            }
        });
        box.valueProperty().addListener((observable, oldValue, value) -> applyFilters());
    }

    public void refresh() {
        if (context.tasks().isBusy()) return;
        Long requestId = selectedRequest() == null ? null : selectedRequest().getId();
        Client selectedClient = clientTable.getSelectionModel().getSelectedItem();
        Long clientId = selectedClient == null ? null : selectedClient.getId();
        Long filterId = clientFilter.getValue() == null ? null : clientFilter.getValue().getId();
        operationStatus.setText("Загружаем клиентов и заявки…");
        context.tasks().submit(() -> new Snapshot(context.clients().findAll(), context.requests().findAll()), snapshot -> {
            clientsById.clear();
            snapshot.clients().forEach(client -> clientsById.put(client.getId(), client));
            clients.setAll(snapshot.clients());
            requests.setAll(snapshot.requests());
            clientFilter.getItems().setAll(Collections.singletonList(null));
            clientFilter.getItems().addAll(clients);
            clientFilter.setValue(filterId == null ? null : clientsById.get(filterId));
            loaded = true;
            applyFilters();
            applyClientSearch();
            if (requestId != null) sortedRequests.stream().filter(r -> r.getId() == requestId).findFirst()
                    .ifPresent(requestTable.getSelectionModel()::select);
            if (clientId != null) filteredClients.stream().filter(c -> c.getId() == clientId).findFirst()
                    .ifPresent(clientTable.getSelectionModel()::select);
            requestTable.refresh();
            clientTable.refresh();
            clientTable.sort();
            showDetails(selectedRequest());
            updateStatistics();
            updateActions();
            operationStatus.setText("Данные обновлены в " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }, error -> {
            operationStatus.setText(loaded ? "Не удалось обновить. Показаны ранее загруженные данные." : "Нет данных. Проверьте подключение и нажмите «Обновить».");
            FxAlerts.error(owner(), "Не удалось загрузить данные", error);
        });
    }

    private void applyFilters() {
        String query = normalize(requestSearch.getText());
        RequestStatus status = statusFilter.getValue();
        ResourceType type = typeFilter.getValue();
        Region region = regionFilter.getValue();
        Client client = clientFilter.getValue();
        filteredRequests.setPredicate(request -> (status == null || request.getStatus() == status)
                && (type == null || request.getResourceType() == type)
                && (region == null || request.getRegion() == region)
                && (client == null || request.getClientId() == client.getId())
                && (query.isEmpty() || requestSearchText(request).contains(query)));
        requestCount.setText("Показано " + filteredRequests.size() + " из " + requests.size());
        updateActions();
    }

    private String requestSearchText(ResourceRequest request) {
        Client client = clientsById.get(request.getClientId());
        return normalize(request.getTitle() + " " + request.getDescription() + " "
                + (client == null ? "" : client.getName() + " " + client.getCompany() + " " + client.getEmail()));
    }

    private void applyClientSearch() {
        String query = normalize(clientSearch.getText());
        filteredClients.setPredicate(client -> normalize(client.getName() + " " + client.getCompany() + " " + client.getEmail()).contains(query));
        clientCount.setText("Показано " + filteredClients.size() + " из " + clients.size());
        updateActions();
    }

    private static String normalize(String text) { return text == null ? "" : text.strip().toLowerCase(Locale.ROOT); }

    @FXML private void clearFilters() {
        requestSearch.clear();
        statusFilter.setValue(null);
        typeFilter.setValue(null);
        regionFilter.setValue(null);
        clientFilter.setValue(null);
        applyFilters();
    }

    @FXML private void addRequest() {
        if (!requestAdd.isDisabled()) EditorDialogs.showRequest(owner(), context, null, List.copyOf(clients), this::refresh);
    }

    @FXML private void editRequest() {
        ResourceRequest request = selectedRequest();
        if (request != null && !requestEdit.isDisabled())
            EditorDialogs.showRequest(owner(), context, request, List.copyOf(clients), this::refresh);
    }

    @FXML private void deleteRequest() {
        ResourceRequest request = selectedRequest();
        if (request == null || requestDelete.isDisabled()) return;
        if (FxAlerts.confirm(owner(), "Удалить заявку?", "Заявка «" + request.getTitle() + "» будет удалена. Это действие нельзя отменить.")) {
            context.tasks().submit(() -> { context.requests().delete(request.getId(), request.getVersion()); return null; },
                    ignored -> refresh(), error -> FxAlerts.error(owner(), "Не удалось удалить заявку", error));
        }
    }

    @FXML private void changeStatus() {
        ResourceRequest request = selectedRequest();
        if (request == null || requestStatus.isDisabled()) return;
        List<StatusChoice> choices = Arrays.stream(RequestStatus.values())
                .filter(request.getStatus()::canTransitionTo).map(StatusChoice::new).toList();
        ChoiceDialog<StatusChoice> dialog = new ChoiceDialog<>(choices.getFirst(), choices);
        dialog.initOwner(owner());
        dialog.setTitle("Изменить статус");
        dialog.setHeaderText(request.getTitle() + "\nТекущий статус: " + Labels.status(request.getStatus()));
        dialog.setContentText("Новый статус:");
        styleDialog(dialog);
        dialog.showAndWait().ifPresent(choice -> context.tasks().submit(() -> {
            context.requests().changeStatus(request.getId(), choice.status(), request.getVersion());
            return null;
        }, ignored -> refresh(), error -> FxAlerts.error(owner(), "Не удалось изменить статус", error)));
    }

    @FXML private void addClient() {
        if (!clientAdd.isDisabled()) EditorDialogs.showClient(owner(), context, null, this::refresh);
    }

    @FXML private void editClient() {
        Client client = clientTable.getSelectionModel().getSelectedItem();
        if (client != null && !clientEdit.isDisabled()) EditorDialogs.showClient(owner(), context, client, this::refresh);
    }

    @FXML private void deleteClient() {
        Client client = clientTable.getSelectionModel().getSelectedItem();
        if (client == null || clientDelete.isDisabled()) return;
        if (FxAlerts.confirm(owner(), "Удалить клиента?", "Клиент «" + client.getName() + "» будет удалён. Это действие нельзя отменить.")) {
            context.tasks().submit(() -> { context.clients().delete(client.getId()); return null; },
                    ignored -> refresh(), error -> FxAlerts.error(owner(), "Не удалось удалить клиента", error));
        }
    }

    @FXML private void showClient() {
        ResourceRequest request = selectedRequest();
        if (request == null) return;
        clientSearch.clear();
        Client client = clientsById.get(request.getClientId());
        tabs.getSelectionModel().select(clientsTab);
        clientTable.getSelectionModel().select(client);
        clientTable.scrollTo(client);
    }

    @FXML private void showClientRequests() {
        Client client = clientTable.getSelectionModel().getSelectedItem();
        if (client == null) return;
        clearFilters();
        clientFilter.setValue(client);
        tabs.getSelectionModel().select(requestsTab);
    }

    @FXML private void exportRequests() {
        if (exportButton.isDisabled()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить текущий список заявок");
        chooser.setInitialFileName("s-cloud-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")) + ".xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Книга Excel (*.xlsx)", "*.xlsx"));
        File chosen = chooser.showSaveDialog(owner());
        if (chosen == null) return;
        File target = chosen.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx") ? chosen : new File(chosen.getPath() + ".xlsx");
        if (target.exists()) {
            FxAlerts.error(owner(), "Выберите новое имя файла", new java.nio.file.FileAlreadyExistsException(target.getAbsolutePath()));
            return;
        }
        List<ResourceRequest> visible = List.copyOf(sortedRequests);
        Set<Long> clientIds = new HashSet<>();
        visible.forEach(request -> clientIds.add(request.getClientId()));
        List<Client> visibleClients = clients.stream().filter(client -> clientIds.contains(client.getId())).toList();
        operationStatus.setText("Сохраняем " + visible.size() + " заявок в Excel…");
        context.tasks().submit(() -> { context.exports().export(target.toPath(), visibleClients, visible); return null; },
                ignored -> {
                    operationStatus.setText("Экспортировано заявок: " + visible.size() + ". Файл: " + target.getAbsolutePath());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.initOwner(owner());
                    alert.setTitle("Экспорт завершён");
                    alert.setHeaderText("Сохранено заявок: " + visible.size());
                    alert.setContentText(target.getAbsolutePath());
                    styleDialog(alert);
                    alert.showAndWait();
                }, error -> {
                    operationStatus.setText("Не удалось сохранить файл.");
                    FxAlerts.error(owner(), "Ошибка экспорта", error);
                });
    }

    private void showDetails(ResourceRequest request) {
        if (request == null) {
            detailTitle.setText("Сведения о заявке");
            detailClient.setText("Выберите строку в таблице, чтобы увидеть клиента и описание.");
            detailDescription.setText("");
            return;
        }
        detailTitle.setText(request.getTitle());
        Client client = clientsById.get(request.getClientId());
        detailClient.setText(client == null ? "Клиент недоступен — обновите данные."
                : client.getName() + "  ·  " + client.getCompany() + "  ·  " + client.getEmail());
        String rule = request.getStatus() == RequestStatus.NEW ? "Параметры доступны для редактирования."
                : "Параметры можно менять только у новой заявки.";
        detailDescription.setText((request.getDescription() == null || request.getDescription().isBlank()
                ? "Описание не указано." : request.getDescription()) + "\n" + rule);
    }

    private void updateActions() {
        boolean unavailable = !loaded || context.tasks().isBusy();
        ResourceRequest request = selectedRequest();
        requestAdd.setDisable(unavailable || clients.isEmpty());
        requestEdit.setDisable(unavailable || request == null || request.getStatus() != RequestStatus.NEW);
        requestDelete.setDisable(unavailable || request == null || request.getStatus() == RequestStatus.APPROVED || request.getStatus() == RequestStatus.PROVISIONED);
        requestStatus.setDisable(unavailable || request == null || Arrays.stream(RequestStatus.values()).noneMatch(next -> request.getStatus().canTransitionTo(next)));
        viewClient.setDisable(unavailable || request == null || !clientsById.containsKey(request.getClientId()));
        exportButton.setDisable(unavailable);
        Client client = clientTable.getSelectionModel().getSelectedItem();
        boolean hasRequests = client != null && requests.stream().anyMatch(r -> r.getClientId() == client.getId());
        clientAdd.setDisable(unavailable);
        clientEdit.setDisable(unavailable || client == null);
        clientDelete.setDisable(unavailable || client == null || hasRequests);
        viewRequests.setDisable(unavailable || client == null);
        clientHint.setText(client == null ? "Выберите клиента для редактирования или просмотра его заявок."
                : hasRequests ? "У клиента есть заявки. Удаление доступно только после удаления всех его заявок." : "У клиента нет заявок — его можно удалить.");
    }

    private void updateStatistics() {
        statisticsCards.getChildren().clear();
        StatisticsService.calculate(List.copyOf(clients), List.copyOf(requests)).forEach((key, value) -> {
            String title = key;
            for (RequestStatus status : RequestStatus.values()) {
                if (key.equals("Заявки " + status.name())) title = Labels.status(status);
            }
            if (key.equals("В работе (NEW + APPROVED)")) title = "В работе · новые и одобренные";
            Label number = new Label(String.format(Locale.forLanguageTag("ru-RU"), "%,d", value));
            number.getStyleClass().add("stat-value");
            Label label = new Label(title);
            label.setWrapText(true);
            label.getStyleClass().add("stat-label");
            VBox card = new VBox(10, label, number);
            card.getStyleClass().add("stat-card");
            card.setPrefWidth(245);
            card.setMinHeight(112);
            statisticsCards.getChildren().add(card);
        });
        statisticsHint.setText("Показатели по всем данным на " + DATE.format(LocalDateTime.now()) + ". Фильтры списка на них не влияют.");
    }

    private void styleDialog(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("app.css")).toExternalForm());
        Button cancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancel != null) cancel.setText("Отмена");
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (ok != null) ok.setText("Готово");
    }

    private ResourceRequest selectedRequest() { return requestTable.getSelectionModel().getSelectedItem(); }
    private Window owner() { return requestTable.getScene().getWindow(); }
    private String clientName(long id) {
        Client client = clientsById.get(id);
        return client == null ? "Клиент недоступен" : client.getName() + " · " + client.getCompany();
    }

    private record Snapshot(List<Client> clients, List<ResourceRequest> requests) { }
    private record StatusChoice(RequestStatus status) {
        @Override public String toString() { return Labels.status(status); }
    }
}
