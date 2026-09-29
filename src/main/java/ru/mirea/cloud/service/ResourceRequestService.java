package ru.mirea.cloud.service;

import ru.mirea.cloud.exception.*;
import ru.mirea.cloud.model.*;
import ru.mirea.cloud.repository.ResourceRequestRepository;
import java.time.LocalDateTime;
import java.util.*;

public final class ResourceRequestService {
    public enum SortOrder { CREATED_ASC, CREATED_DESC, STORAGE_DESC, TITLE_ASC }

    private final ResourceRequestRepository requests;
    private final ClientService clients;

    public ResourceRequestService(ResourceRequestRepository requests, ClientService clients) {
        this.requests = requests;
        this.clients = clients;
    }

    public List<ResourceRequest> findAll() { return requests.findAll(); }

    public ResourceRequest get(long id) {
        Validation.positiveId(id);
        return requests.findById(id).orElseThrow(() -> new EntityNotFoundException("Заявка", id));
    }

    public ResourceRequest create(long clientId, String title, String description, ResourceType type,
                                  Region region, int cpu, int ram, int storage) {
        return requests.create(validated(0, clientId, title, description, type, region, cpu, ram, storage,
                RequestStatus.NEW, LocalDateTime.now(), 0));
    }

    public void update(long id, long clientId, String title, String description, ResourceType type,
                       Region region, int cpu, int ram, int storage) {
        ResourceRequest current = get(id);
        if (current.getStatus() != RequestStatus.NEW)
            throw new BusinessException("Параметры можно менять только у новой заявки (NEW).");
        requests.update(validated(id, clientId, title, description, type, region, cpu, ram, storage,
                current.getStatus(), current.getCreatedAt(), current.getVersion()));
    }

    public void changeStatus(long id, RequestStatus next) {
        ResourceRequest current = get(id);
        if (next == null || !current.getStatus().canTransitionTo(next))
            throw new BusinessException("Запрещён переход " + current.getStatus() + " → " + next + ".");
        requests.update(current.withStatus(next));
    }

    public void delete(long id) {
        ResourceRequest current = get(id);
        if (current.getStatus() == RequestStatus.APPROVED || current.getStatus() == RequestStatus.PROVISIONED)
            throw new BusinessException("Нельзя удалить одобренную заявку или заявку с выделенным ресурсом.");
        requests.delete(id, current.getVersion());
    }

    public List<ResourceRequest> searchTitle(String query) {
        String term = Validation.required(query, "Поисковая строка", 150).toLowerCase(Locale.ROOT);
        return findAll().stream().filter(r -> r.getTitle().toLowerCase(Locale.ROOT).contains(term)).toList();
    }

    public List<ResourceRequest> searchDescription(String query) {
        String term = Validation.required(query, "Поисковая строка", 2000).toLowerCase(Locale.ROOT);
        return findAll().stream().filter(r -> r.getDescription().toLowerCase(Locale.ROOT).contains(term)).toList();
    }

    public List<ResourceRequest> searchClient(long clientId) {
        clients.get(clientId);
        return findAll().stream().filter(r -> r.getClientId() == clientId).toList();
    }

    public List<ResourceRequest> filter(RequestStatus status, ResourceType type, Region region) {
        return findAll().stream().filter(r -> status == null || r.getStatus() == status)
                .filter(r -> type == null || r.getResourceType() == type)
                .filter(r -> region == null || r.getRegion() == region).toList();
    }

    public List<ResourceRequest> sort(SortOrder order) {
        if (order == null) throw new BusinessException("Выберите способ сортировки.");
        Comparator<ResourceRequest> comparator = switch (order) {
            case CREATED_ASC -> Comparator.comparing(ResourceRequest::getCreatedAt);
            case CREATED_DESC -> Comparator.comparing(ResourceRequest::getCreatedAt).reversed();
            case STORAGE_DESC -> Comparator.comparingInt(ResourceRequest::getStorageGb).reversed();
            case TITLE_ASC -> Comparator.comparing(ResourceRequest::getTitle, String.CASE_INSENSITIVE_ORDER);
        };
        return findAll().stream().sorted(comparator.thenComparingLong(ResourceRequest::getId)).toList();
    }

    private ResourceRequest validated(long id, long clientId, String title, String description,
                                      ResourceType type, Region region, int cpu, int ram, int storage,
                                      RequestStatus status, LocalDateTime created, int version) {
        title = Validation.required(title, "Название", 150);
        description = description == null ? "" : description.strip();
        if (description.length() > 2000) throw new BusinessException("Описание: максимум 2000 символов.");
        clients.get(clientId);
        if (type == null || region == null) throw new BusinessException("Тип ресурса и регион обязательны.");
        if (storage < 10 || storage > 10000) throw new BusinessException("Объем диска: от 10 до 10000 ГБ.");
        if (type == ResourceType.OBJECT_STORAGE) {
            if (cpu != 0 || ram != 0) throw new BusinessException("Объектное хранилище: CPU и RAM должны быть равны 0.");
        } else {
            if (cpu < 1 || cpu > 64) throw new BusinessException("CPU: от 1 до 64 ядер.");
            if (ram < 1 || ram > 512 || ram < cpu * 2)
                throw new BusinessException("RAM: до 512 ГБ и не менее 2 ГБ на ядро CPU.");
        }
        return new ResourceRequest(id, clientId, title, description, type, region, cpu, ram, storage, status, created, version);
    }
}
