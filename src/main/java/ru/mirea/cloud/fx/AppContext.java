package ru.mirea.cloud.fx;

import ru.mirea.cloud.repository.JdbcClientRepository;
import ru.mirea.cloud.repository.JdbcResourceRequestRepository;
import ru.mirea.cloud.service.ClientService;
import ru.mirea.cloud.service.ExportService;
import ru.mirea.cloud.service.ResourceRequestService;
import ru.mirea.cloud.util.DatabaseManager;
import ru.mirea.cloud.util.ExcelExporter;

/** Shared services; the JavaFX client connects directly to the KR1 PostgreSQL database. */
public record AppContext(ClientService clients, ResourceRequestService requests,
                         ExportService exports, FxTasks tasks) implements AutoCloseable {
    public static AppContext create() {
        DatabaseManager database = DatabaseManager.fromEnvironment();
        var clientRepository = new JdbcClientRepository(database);
        var requestRepository = new JdbcResourceRequestRepository(database);
        var clients = new ClientService(clientRepository, requestRepository);
        var requests = new ResourceRequestService(requestRepository, clients);
        return new AppContext(clients, requests,
                new ExportService(clients, requests, new ExcelExporter()), new FxTasks());
    }

    @Override public void close() { tasks.close(); }
}
