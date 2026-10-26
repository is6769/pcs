package ru.mirea.cloud;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.cloud.exception.*;
import ru.mirea.cloud.model.*;
import ru.mirea.cloud.repository.*;
import ru.mirea.cloud.service.*;
import ru.mirea.cloud.ui.ConsoleUi;
import ru.mirea.cloud.util.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Runs against real PostgreSQL in a unique temporary schema; never clears public data. */
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class JdbcIntegrationTest {
    private static DatabaseManager admin;
    private static DatabaseManager database;
    private static String schema;
    private ClientService clients;
    private ResourceRequestService requests;
    private JdbcResourceRequestRepository requestRepository;

    @BeforeAll static void createSchema() throws Exception {
        admin = DatabaseManager.fromEnvironment();
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = admin.open(); PreparedStatement s = c.prepareStatement("CREATE SCHEMA " + schema)) { s.execute(); }
        String url = configured("url");
        database = new DatabaseManager(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                configured("user"), configured("password"));
        try (Connection c = database.open()) {
            for (String script : List.of("sql/01-schema.sql", "sql/02-seed.sql")) {
                for (String sql : Files.readString(Path.of(script)).split(";")) {
                    if (!sql.isBlank()) try (PreparedStatement s = c.prepareStatement(sql)) { s.execute(); }
                }
            }
        }
    }

    @AfterAll static void dropSchema() throws Exception {
        if (admin != null && schema != null) {
            try (Connection c = admin.open(); PreparedStatement s = c.prepareStatement("DROP SCHEMA IF EXISTS " + schema + " CASCADE")) { s.execute(); }
        }
    }

    @BeforeEach void wire() {
        requestRepository = new JdbcResourceRequestRepository(database);
        clients = new ClientService(new JdbcClientRepository(database), requestRepository);
        requests = new ResourceRequestService(requestRepository, clients);
    }

    @Test void seedQueriesAndExport(@TempDir Path directory) throws Exception {
        assertEquals(5, clients.findAll().size());
        assertEquals(10, requests.findAll().size());
        assertEquals(3, requests.filter(RequestStatus.NEW, null, null).size());
        assertEquals(2, requests.searchClient(1).size());
        assertEquals(2, requests.searchDescription("PostgreSQL").size());
        assertEquals(3200L, new StatisticsService(clients, requests).calculate().get("Выделено диска, ГБ"));
        Path export = directory.resolve("demo.xlsx");
        new ExportService(clients, requests, new ExcelExporter()).export(export);
        try (InputStream stream = Files.newInputStream(export); Workbook workbook = new XSSFWorkbook(stream)) {
            assertEquals(3, workbook.getNumberOfSheets());
            assertEquals(5, workbook.getSheet("Клиенты").getLastRowNum());
            Sheet sheet = workbook.getSheet("Заявки");
            assertEquals(10, sheet.getLastRowNum());
            assertEquals("Веб-сервер магазина", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals(CellType.NUMERIC, sheet.getRow(1).getCell(6).getCellType());
            assertTrue(DateUtil.isCellDateFormatted(sheet.getRow(1).getCell(10)));
            assertTrue(sheet.getPaneInformation().isFreezePane());
        }
        assertThrows(FileAlreadyExistsException.class, () -> new ExportService(clients, requests, new ExcelExporter()).export(export));
    }

    @Test void crudPersistsAndRejectsStaleUpdates() {
        Client client = clients.create("Тест", "jdbc@example.com", "Проверка");
        ResourceRequest request = requests.create(client.getId(), "' OR 1=1 --", "=SUM(1,2)", ResourceType.VIRTUAL_MACHINE, Region.RU_KAZAN, 1, 2, 10);
        try {
            assertEquals("' OR 1=1 --", requests.get(request.getId()).getTitle());
            clients.update(client.getId(), "Новое имя", "jdbc@example.com", "Проверка");
            assertEquals("Новое имя", clients.get(client.getId()).getName());
            requests.update(request.getId(), client.getId(), "Изменена", "", ResourceType.DATABASE, Region.RU_SPB, 2, 8, 50);
            assertThrows(BusinessException.class, () -> requestRepository.update(request.withStatus(RequestStatus.APPROVED)));
            assertThrows(BusinessException.class, () -> requestRepository.delete(request.getId(), request.getVersion()));
            // A second UI still holds version 0. The service must not silently reload version 1 and save over it.
            ResourceRequestService staleUi = new ResourceRequestService(new JdbcResourceRequestRepository(database), clients);
            assertThrows(BusinessException.class, () -> staleUi.update(request.getId(), client.getId(),
                    "Устаревшая форма", "", ResourceType.VIRTUAL_MACHINE, Region.RU_MOSCOW, 1, 2, 10,
                    request.getVersion()));
            assertThrows(BusinessException.class,
                    () -> staleUi.changeStatus(request.getId(), RequestStatus.APPROVED, request.getVersion()));
            assertThrows(BusinessException.class, () -> staleUi.delete(request.getId(), request.getVersion()));
            ResourceRequest refreshed = requests.get(request.getId());
            assertEquals("Изменена", refreshed.getTitle());
            assertEquals(1, refreshed.getVersion());
            assertEquals(RequestStatus.NEW, refreshed.getStatus());
            requests.changeStatus(request.getId(), RequestStatus.APPROVED, refreshed.getVersion());
            assertThrows(BusinessException.class, () -> requests.delete(request.getId()));
            requests.changeStatus(request.getId(), RequestStatus.CANCELLED);
            assertEquals(RequestStatus.CANCELLED, new JdbcResourceRequestRepository(database).findById(request.getId()).orElseThrow().getStatus());
        } finally {
            ResourceRequest current = requests.get(request.getId());
            requestRepository.delete(current.getId(), current.getVersion());
            clients.delete(client.getId());
        }
        assertThrows(EntityNotFoundException.class, () -> requests.get(request.getId()));
    }

    @Test void databaseConstraintsProtectData() throws Exception {
        try (Connection c = database.open()) {
            assertSqlState(c, "23505", "INSERT INTO clients(name,email,company) VALUES ('X','anna@example.com','X')");
            assertSqlState(c, "23502", "INSERT INTO clients(name,email,company) VALUES (NULL,'x@example.com','X')");
            assertSqlState(c, "23503", "DELETE FROM clients WHERE id=1");
            assertSqlState(c, "23503", "UPDATE resource_requests SET client_id=999999 WHERE id=1");
            assertSqlState(c, "23514", "UPDATE resource_requests SET status='INVALID' WHERE id=1");
            assertSqlState(c, "23514", "UPDATE resource_requests SET ram_gb=1 WHERE id=1");
        }
    }

    @Test void uiSurvivesInputAndBusinessErrors() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleUi ui = new ConsoleUi(new Scanner("abc\n2\n3\n999999\n6\n1\n3\n2\n0\n5\n0\n"), new PrintStream(output),
                clients, requests, new StatisticsService(clients, requests), new ExportService(clients, requests, new ExcelExporter()), database);
        ui.run();
        String text = output.toString();
        assertTrue(text.contains("не найден"));
        assertTrue(text.contains("Запрещён переход"));
        assertTrue(text.contains("Всего заявок: 10"));
        assertTrue(text.contains("До свидания"));
    }

    private static void assertSqlState(Connection c, String state, String sql) {
        SQLException exception = assertThrows(SQLException.class, () -> {
            try (PreparedStatement s = c.prepareStatement(sql)) { s.executeUpdate(); }
        });
        assertEquals(state, exception.getSQLState());
    }

    /** fromEnvironment() above has already validated the same required settings. */
    private static String configured(String key) {
        String value = System.getProperty("db." + key);
        if (value == null) value = System.getenv("DB_" + key.toUpperCase(Locale.ROOT));
        return key.equals("password") ? value : value.strip();
    }
}
