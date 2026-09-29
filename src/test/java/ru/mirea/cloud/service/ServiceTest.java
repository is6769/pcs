package ru.mirea.cloud.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import ru.mirea.cloud.exception.*;
import ru.mirea.cloud.model.*;
import ru.mirea.cloud.repository.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {
    private MemoryClients clientRepository;
    private MemoryRequests requestRepository;
    private ClientService clients;
    private ResourceRequestService requests;

    @BeforeEach void setup() {
        clientRepository = new MemoryClients();
        requestRepository = new MemoryRequests();
        clients = new ClientService(clientRepository, requestRepository);
        requests = new ResourceRequestService(requestRepository, clients);
        clients.create("Анна", "anna@example.com", "Компания");
    }

    private ResourceRequest create() {
        return requests.create(1, "Веб-сервер", "Каталог товаров", ResourceType.VIRTUAL_MACHINE, Region.RU_MOSCOW, 2, 4, 100);
    }

    @Test void normalizesEmailAndRejectsDuplicate() {
        Client second = clients.create("Борис", " BOB@EXAMPLE.COM ", "Бета");
        assertEquals("bob@example.com", second.getEmail());
        assertThrows(BusinessException.class, () -> clients.create("Другой", "BOB@example.com", "Бета"));
        clients.update(second.getId(), "Борис", "bob@example.com", "Новая компания");
        assertEquals("Новая компания", clients.get(second.getId()).getCompany());
    }

    @ParameterizedTest @ValueSource(strings = {"", " ", "abc", "a@b", "a b@c.ru"})
    void rejectsInvalidEmail(String email) {
        assertThrows(BusinessException.class, () -> clients.create("Имя", email, "Компания"));
    }

    @Test void mandatoryFieldsAndMissingClient() {
        assertThrows(BusinessException.class, () -> clients.create(" ", "x@example.com", "Компания"));
        assertThrows(BusinessException.class, () -> clients.create("Имя", "x@example.com", ""));
        assertThrows(BusinessException.class, () -> requests.create(1, " ", "", ResourceType.DATABASE, Region.RU_SPB, 2, 4, 10));
        assertThrows(EntityNotFoundException.class, () -> requests.create(999, "База", "", ResourceType.DATABASE, Region.RU_SPB, 2, 4, 10));
        assertThrows(BusinessException.class, () -> requests.create(1, "База", "", null, Region.RU_SPB, 2, 4, 10));
        assertThrows(BusinessException.class, () -> requests.create(1, "База", "", ResourceType.DATABASE, null, 2, 4, 10));
    }

    @ParameterizedTest @CsvSource({"0,4,10", "65,130,10", "4,7,10", "1,513,10", "1,2,9", "1,2,10001", "1,-1,10"})
    void rejectsInvalidCapacity(int cpu, int ram, int storage) {
        assertThrows(BusinessException.class, () -> requests.create(1, "База", "", ResourceType.DATABASE, Region.RU_SPB, cpu, ram, storage));
        assertTrue(requests.findAll().isEmpty());
    }

    @Test void objectStorageAndBoundaryValues() {
        assertThrows(BusinessException.class, () -> requests.create(1, "Архив", "", ResourceType.OBJECT_STORAGE, Region.RU_SPB, 1, 2, 10));
        assertEquals(0, requests.create(1, "Архив", "", ResourceType.OBJECT_STORAGE, Region.RU_SPB, 0, 0, 10000).getCpu());
        assertEquals(512, requests.create(1, "База", "", ResourceType.DATABASE, Region.RU_SPB, 64, 512, 10).getRamGb());
    }

    @Test void validatesAllStatusTransitions() {
        Set<String> allowed = Set.of("NEW:APPROVED", "NEW:REJECTED", "NEW:CANCELLED", "APPROVED:PROVISIONED", "APPROVED:CANCELLED");
        for (RequestStatus from : RequestStatus.values()) {
            for (RequestStatus to : RequestStatus.values()) {
                assertEquals(allowed.contains(from + ":" + to), from.canTransitionTo(to), from + " -> " + to);
            }
        }
        ResourceRequest request = create();
        assertEquals(RequestStatus.NEW, request.getStatus());
        assertThrows(BusinessException.class, () -> requests.changeStatus(request.getId(), RequestStatus.PROVISIONED));
        assertThrows(BusinessException.class, () -> requests.changeStatus(request.getId(), null));
        requests.changeStatus(request.getId(), RequestStatus.APPROVED);
        requests.changeStatus(request.getId(), RequestStatus.PROVISIONED);
        assertThrows(BusinessException.class, () -> requests.changeStatus(request.getId(), RequestStatus.CANCELLED));
    }

    @Test void editsOnlyNewAndPreservesCreationDate() {
        ResourceRequest request = create();
        requests.update(request.getId(), 1, "Новая база", "Описание", ResourceType.DATABASE, Region.RU_SPB, 4, 8, 200);
        assertEquals("Новая база", requests.get(request.getId()).getTitle());
        assertEquals(request.getCreatedAt(), requests.get(request.getId()).getCreatedAt());
        requests.changeStatus(request.getId(), RequestStatus.APPROVED);
        assertThrows(BusinessException.class, () -> requests.update(request.getId(), 1, "Название", "", ResourceType.DATABASE, Region.RU_SPB, 4, 8, 200));
    }

    @Test void deletionRulesAndMissingRecords() {
        ResourceRequest first = create();
        assertThrows(BusinessException.class, () -> clients.delete(1));
        requests.changeStatus(first.getId(), RequestStatus.APPROVED);
        assertThrows(BusinessException.class, () -> requests.delete(first.getId()));
        requests.changeStatus(first.getId(), RequestStatus.CANCELLED);
        requests.delete(first.getId());
        assertThrows(EntityNotFoundException.class, () -> requests.get(first.getId()));
        ResourceRequest second = create();
        requests.changeStatus(second.getId(), RequestStatus.APPROVED);
        requests.changeStatus(second.getId(), RequestStatus.PROVISIONED);
        assertThrows(BusinessException.class, () -> requests.delete(second.getId()));
        assertThrows(BusinessException.class, () -> requests.get(-1));
        Client unused = clients.create("Вера", "vera@example.com", "Гамма");
        clients.delete(unused.getId());
        assertThrows(EntityNotFoundException.class, () -> clients.get(unused.getId()));
    }

    @Test void queriesAndStatisticsHaveExpectedMeaning() {
        ResourceRequest first = create();
        ResourceRequest archive = requests.create(1, "Архив", "КАТАЛОГ копий", ResourceType.OBJECT_STORAGE, Region.RU_SPB, 0, 0, 2000);
        requests.changeStatus(first.getId(), RequestStatus.APPROVED);
        requests.changeStatus(first.getId(), RequestStatus.PROVISIONED);
        assertEquals(1, requests.searchTitle("ВЕБ").size());
        assertEquals(2, requests.searchDescription("каталог").size());
        assertEquals(2, requests.searchClient(1).size());
        assertTrue(requests.searchTitle("ничего").isEmpty());
        assertEquals(List.of(archive), requests.filter(RequestStatus.NEW, ResourceType.OBJECT_STORAGE, Region.RU_SPB));
        assertTrue(requests.filter(RequestStatus.NEW, ResourceType.DATABASE, null).isEmpty());
        assertEquals(archive.getId(), requests.sort(ResourceRequestService.SortOrder.STORAGE_DESC).getFirst().getId());
        assertEquals(first.getId(), requests.sort(ResourceRequestService.SortOrder.CREATED_ASC).getFirst().getId());
        Map<String, Long> stats = new StatisticsService(clients, requests).calculate();
        assertEquals(2L, stats.get("Всего заявок"));
        assertEquals(1L, stats.get("В работе (NEW + APPROVED)"));
        assertEquals(100L, stats.get("Выделено диска, ГБ"));
        assertEquals(2L, stats.get("Выделено CPU, ядер"));
    }

    // Alternative interface implementations demonstrate polymorphism without a database.
    private static final class MemoryClients implements ClientRepository {
        private final Map<Long, Client> data = new LinkedHashMap<>();
        private long sequence;
        public Client create(Client c) { Client saved = new Client(++sequence, c.getName(), c.getEmail(), c.getCompany()); data.put(saved.getId(), saved); return saved; }
        public List<Client> findAll() { return new ArrayList<>(data.values()); }
        public Optional<Client> findById(long id) { return Optional.ofNullable(data.get(id)); }
        public boolean emailExists(String email, long id) { return data.values().stream().anyMatch(c -> c.getId() != id && c.getEmail().equals(email)); }
        public void update(Client c) { data.put(c.getId(), c); }
        public void delete(long id) { data.remove(id); }
    }

    private static final class MemoryRequests implements ResourceRequestRepository {
        private final Map<Long, ResourceRequest> data = new LinkedHashMap<>();
        private long sequence;
        public ResourceRequest create(ResourceRequest r) {
            ResourceRequest saved = new ResourceRequest(++sequence, r.getClientId(), r.getTitle(), r.getDescription(), r.getResourceType(), r.getRegion(), r.getCpu(), r.getRamGb(), r.getStorageGb(), r.getStatus(), r.getCreatedAt(), 0);
            data.put(saved.getId(), saved); return saved;
        }
        public List<ResourceRequest> findAll() { return new ArrayList<>(data.values()); }
        public Optional<ResourceRequest> findById(long id) { return Optional.ofNullable(data.get(id)); }
        public void update(ResourceRequest r) { data.put(r.getId(), r); }
        public void delete(long id, int version) { data.remove(id); }
        public boolean existsByClient(long id) { return data.values().stream().anyMatch(r -> r.getClientId() == id); }
    }
}
