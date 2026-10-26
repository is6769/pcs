package ru.mirea.cloud.service;

import ru.mirea.cloud.util.ExcelExporter;
import ru.mirea.cloud.model.Client;
import ru.mirea.cloud.model.ResourceRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ExportService {
    private final ClientService clients;
    private final ResourceRequestService requests;
    private final ExcelExporter exporter;

    public ExportService(ClientService clients, ResourceRequestService requests, ExcelExporter exporter) {
        this.clients = clients;
        this.requests = requests;
        this.exporter = exporter;
    }

    public void export(Path path) throws IOException {
        exporter.export(path, clients.findAll(), requests.findAll());
    }

    /** Exports the current UI snapshot in its displayed order, without re-querying the database. */
    public void export(Path path, List<Client> currentClients, List<ResourceRequest> currentRequests) throws IOException {
        exporter.export(path, List.copyOf(currentClients), List.copyOf(currentRequests));
    }
}
