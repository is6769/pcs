package ru.mirea.cloud.service;

import ru.mirea.cloud.model.*;
import java.util.*;

public final class StatisticsService {
    private final ClientService clients;
    private final ResourceRequestService requests;

    public StatisticsService(ClientService clients, ResourceRequestService requests) {
        this.clients = clients;
        this.requests = requests;
    }

    public Map<String, Long> calculate() { return calculate(clients.findAll(), requests.findAll()); }

    public static Map<String, Long> calculate(List<Client> clients, List<ResourceRequest> requests) {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("Всего клиентов", (long) clients.size());
        result.put("Всего заявок", (long) requests.size());
        for (RequestStatus status : RequestStatus.values()) {
            result.put("Заявки " + status, requests.stream().filter(r -> r.getStatus() == status).count());
        }
        result.put("В работе (NEW + APPROVED)", requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.NEW || r.getStatus() == RequestStatus.APPROVED).count());
        result.put("Выделено CPU, ядер", requests.stream().filter(r -> r.getStatus() == RequestStatus.PROVISIONED)
                .mapToLong(ResourceRequest::getCpu).sum());
        result.put("Выделено RAM, ГБ", requests.stream().filter(r -> r.getStatus() == RequestStatus.PROVISIONED)
                .mapToLong(ResourceRequest::getRamGb).sum());
        result.put("Выделено диска, ГБ", requests.stream().filter(r -> r.getStatus() == RequestStatus.PROVISIONED)
                .mapToLong(ResourceRequest::getStorageGb).sum());
        return result;
    }
}
