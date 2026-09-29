package ru.mirea.cloud.repository;

import ru.mirea.cloud.model.ResourceRequest;
import java.util.*;

public interface ResourceRequestRepository {
    ResourceRequest create(ResourceRequest request);
    List<ResourceRequest> findAll();
    Optional<ResourceRequest> findById(long id);
    void update(ResourceRequest request);
    void delete(long id, int version);
    boolean existsByClient(long clientId);
}
