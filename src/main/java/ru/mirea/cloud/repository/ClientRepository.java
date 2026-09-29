package ru.mirea.cloud.repository;

import ru.mirea.cloud.model.Client;
import java.util.*;

public interface ClientRepository {
    Client create(Client client);
    List<Client> findAll();
    Optional<Client> findById(long id);
    boolean emailExists(String email, long excludingId);
    void update(Client client);
    void delete(long id);
}
