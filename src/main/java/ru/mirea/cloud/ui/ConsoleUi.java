package ru.mirea.cloud.ui;

import ru.mirea.cloud.exception.*;
import ru.mirea.cloud.model.*;
import ru.mirea.cloud.service.*;
import ru.mirea.cloud.util.DatabaseManager;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ConsoleUi {
    private final ConsoleInput input;
    private final PrintStream out;
    private final ClientService clients;
    private final ResourceRequestService requests;
    private final StatisticsService statistics;
    private final ExportService exports;
    private final DatabaseManager database;

    public ConsoleUi(Scanner scanner, PrintStream out, ClientService clients, ResourceRequestService requests,
                     StatisticsService statistics, ExportService exports, DatabaseManager database) {
        this.input = new ConsoleInput(scanner, out);
        this.out = out;
        this.clients = clients;
        this.requests = requests;
        this.statistics = statistics;
        this.exports = exports;
        this.database = database;
    }

    public void run() {
        out.println("Система учета заявок. Ресурсы в реальном облаке не создаются.");
        try {
            menu("ОБЛАЧНЫЙ ПРОВАЙДЕР", """
                    1. Клиенты
                    2. Заявки на облачные ресурсы
                    3. Поиск
                    4. Фильтрация
                    5. Статистика
                    6. Экспорт в Excel
                    7. Вывести таблицы базы данных
                    8. Сортировка заявок
                    0. Выход
                    """, action -> {
                switch (action) {
                    case 1 -> clientsMenu();
                    case 2 -> requestsMenu();
                    case 3 -> searchMenu();
                    case 4 -> filterMenu();
                    case 5 -> statistics.calculate().forEach((key, value) -> out.println(key + ": " + value));
                    case 6 -> export();
                    case 7 -> {
                        print(database.listTables());
                        out.println("Содержимое clients:"); print(clients.findAll());
                        out.println("Содержимое resource_requests:"); print(requests.findAll());
                    }
                    case 8 -> print(requests.sort(input.choice("Сортировка", ResourceRequestService.SortOrder.class)));
                    default -> unknown();
                }
            });
        } catch (NoSuchElementException e) { out.println("\nВвод завершён."); }
        out.println("До свидания!");
    }

    private void clientsMenu() {
        menu("КЛИЕНТЫ", "1. Создать\n2. Все клиенты\n3. По ID\n4. Изменить\n5. Удалить\n0. Назад", action -> {
            switch (action) {
                case 1 -> out.println("Создан клиент: " + clients.create(input.text("Имя"), input.text("Email"), input.text("Компания")));
                case 2 -> print(clients.findAll());
                case 3 -> out.println(clients.get(input.id()));
                case 4 -> {
                    long id = input.id(); out.println(clients.get(id));
                    clients.update(id, input.text("Новое имя"), input.text("Новый email"), input.text("Компания"));
                    out.println("Клиент обновлён.");
                }
                case 5 -> {
                    long id = input.id(); out.println(clients.get(id));
                    if (confirmDelete()) { clients.delete(id); out.println("Клиент удалён."); }
                }
                default -> unknown();
            }
        });
    }

    private void requestsMenu() {
        menu("ЗАЯВКИ НА ОБЛАЧНЫЕ РЕСУРСЫ", """
                1. Создать
                2. Все заявки
                3. По ID
                4. Изменить параметры
                5. Удалить
                6. Изменить статус
                7. Сортировка
                0. Назад
                """, action -> {
            switch (action) {
                case 1 -> editRequest(0);
                case 2 -> print(requests.findAll());
                case 3 -> out.println(requests.get(input.id()));
                case 4 -> {
                    long id = input.id(); out.println(requests.get(id)); editRequest(id);
                }
                case 5 -> {
                    long id = input.id(); out.println(requests.get(id));
                    if (confirmDelete()) { requests.delete(id); out.println("Заявка удалена."); }
                }
                case 6 -> {
                    long id = input.id(); out.println(requests.get(id));
                    requests.changeStatus(id, input.choice("Новый статус", RequestStatus.class));
                    out.println("Статус обновлён.");
                }
                case 7 -> print(requests.sort(input.choice("Сортировка", ResourceRequestService.SortOrder.class)));
                default -> unknown();
            }
        });
    }

    private void editRequest(long id) {
        out.println("Клиент заявки:"); long clientId = input.id();
        String title = input.text("Название");
        String description = input.text("Описание (можно пустое)");
        ResourceType type = input.choice("Тип ресурса", ResourceType.class);
        Region region = input.choice("Регион", Region.class);
        out.println("Для OBJECT_STORAGE CPU=0, RAM=0; иначе CPU 1..64, RAM >= 2*CPU и <=512.");
        int cpu = input.number("CPU, ядер");
        int ram = input.number("RAM, ГБ");
        int storage = input.number("Диск, ГБ (10..10000)");
        if (id == 0) out.println("Создана заявка: " + requests.create(clientId, title, description, type, region, cpu, ram, storage));
        else { requests.update(id, clientId, title, description, type, region, cpu, ram, storage); out.println("Заявка обновлена."); }
    }

    private void searchMenu() {
        menu("ПОИСК ЗАЯВОК", "1. По названию\n2. По описанию\n3. По ID клиента\n0. Назад", action -> {
            switch (action) {
                case 1 -> print(requests.searchTitle(input.text("Часть названия")));
                case 2 -> print(requests.searchDescription(input.text("Часть описания")));
                case 3 -> print(requests.searchClient(input.id()));
                default -> unknown();
            }
        });
    }

    private void filterMenu() {
        menu("ФИЛЬТРАЦИЯ ЗАЯВОК", "1. По статусу\n2. По типу ресурса\n3. По региону\n4. Статус + тип + регион\n0. Назад", action -> {
            switch (action) {
                case 1 -> print(requests.filter(input.choice("Статус", RequestStatus.class), null, null));
                case 2 -> print(requests.filter(null, input.choice("Тип", ResourceType.class), null));
                case 3 -> print(requests.filter(null, null, input.choice("Регион", Region.class)));
                case 4 -> print(requests.filter(input.choice("Статус", RequestStatus.class),
                        input.choice("Тип", ResourceType.class), input.choice("Регион", Region.class)));
                default -> unknown();
            }
        });
    }

    private void export() throws IOException {
        Path path = Path.of("exports", "cloud-resources-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")) + ".xlsx");
        exports.export(path);
        out.println("Сохранено: " + path.toAbsolutePath());
    }

    private boolean confirmDelete() { return input.text("Удалить? Введите ДА").equalsIgnoreCase("да"); }
    private void unknown() { out.println("Ошибка: неизвестный пункт меню."); }
    private void print(List<?> items) {
        if (items.isEmpty()) out.println("Записи не найдены.");
        else { items.forEach(out::println); out.println("Найдено: " + items.size()); }
    }

    private void menu(String title, String options, MenuAction handler) {
        while (true) {
            out.println("\n========== " + title + " ==========");
            out.println(options);
            int action = input.number("Выберите действие");
            if (action == 0) return;
            try { handler.run(action); }
            catch (BusinessException | DataAccessException e) { out.println("Ошибка: " + e.getMessage()); }
            catch (IOException e) { out.println("Ошибка экспорта: " + e.getMessage()); }
        }
    }

    @FunctionalInterface
    private interface MenuAction { void run(int action) throws IOException; }
}
