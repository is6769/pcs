package ru.mirea.cloud.model;

import java.time.LocalDateTime;

/** Immutable snapshot. Changes are validated by the service and saved with a version check. */
public final class ResourceRequest {
    private final long id;
    private final long clientId;
    private final String title;
    private final String description;
    private final ResourceType resourceType;
    private final Region region;
    private final int cpu;
    private final int ramGb;
    private final int storageGb;
    private final RequestStatus status;
    private final LocalDateTime createdAt;
    private final int version;

    public ResourceRequest(long id, long clientId, String title, String description,
                           ResourceType resourceType, Region region, int cpu, int ramGb,
                           int storageGb, RequestStatus status, LocalDateTime createdAt, int version) {
        this.id = id;
        this.clientId = clientId;
        this.title = title;
        this.description = description;
        this.resourceType = resourceType;
        this.region = region;
        this.cpu = cpu;
        this.ramGb = ramGb;
        this.storageGb = storageGb;
        this.status = status;
        this.createdAt = createdAt;
        this.version = version;
    }

    public ResourceRequest withStatus(RequestStatus next) {
        return new ResourceRequest(id, clientId, title, description, resourceType, region,
                cpu, ramGb, storageGb, next, createdAt, version);
    }

    public long getId() { return id; }
    public long getClientId() { return clientId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ResourceType getResourceType() { return resourceType; }
    public Region getRegion() { return region; }
    public int getCpu() { return cpu; }
    public int getRamGb() { return ramGb; }
    public int getStorageGb() { return storageGb; }
    public RequestStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getVersion() { return version; }

    @Override public String toString() {
        return "%d | клиент=%d | %s | %s | %s | CPU=%d RAM=%d ГБ диск=%d ГБ | %s | %s%n  %s"
                .formatted(id, clientId, title, resourceType, region, cpu, ramGb, storageGb,
                        status, createdAt, description);
    }
}
