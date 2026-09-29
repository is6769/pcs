package ru.mirea.cloud.service;

import ru.mirea.cloud.exception.*;
import ru.mirea.cloud.model.Client;
import ru.mirea.cloud.repository.*;
import java.util.*;

public final class ClientService {
    private final ClientRepository clients;
    private final ResourceRequestRepository requests;

    public ClientService(ClientRepository clients, ResourceRequestRepository requests) {
        this.clients = clients;
        this.requests = requests;
    }

    public List<Client> findAll() { return clients.findAll(); }

    public Client get(long id) {
        Validation.positiveId(id);
        return clients.findById(id).orElseThrow(() -> new EntityNotFoundException("Клиент", id));
    }

    public Client create(String name, String email, String company) {
        return clients.create(validated(0, name, email, company));
    }

    public void update(long id, String name, String email, String company) {
        get(id);
        clients.update(validated(id, name, email, company));
    }

    public void delete(long id) {
        get(id);
        if (requests.existsByClient(id)) throw new BusinessException("Нельзя удалить клиента, у которого есть заявки.");
        clients.delete(id);
    }

    private Client validated(long id, String name, String email, String company) {
        name = Validation.required(name, "Имя", 120);
        email = Validation.required(email, "Email", 254).toLowerCase(Locale.ROOT);
        company = Validation.required(company, "Компания", 150);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new BusinessException("Некорректный email.");
        if (clients.emailExists(email, id)) throw new BusinessException("Этот email уже зарегистрирован.");
        return new Client(id, name, email, company);
    }
}
