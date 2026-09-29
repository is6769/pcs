package ru.mirea.cloud.service;

import ru.mirea.cloud.util.ExcelExporter;
import java.io.IOException;
import java.nio.file.Path;

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
}
