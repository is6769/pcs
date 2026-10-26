package ru.mirea.cloud;

import ru.mirea.cloud.exception.DataAccessException;
import ru.mirea.cloud.repository.*;
import ru.mirea.cloud.service.*;
import ru.mirea.cloud.ui.ConsoleUi;
import ru.mirea.cloud.util.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Scanner;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        if (args.length == 0) {
            DesktopLauncher.main(args);
            return;
        }
        if (args.length == 1 && args[0].equals("--console")) args = new String[0];
        DatabaseManager database = DatabaseManager.fromEnvironment();
        ClientRepository clientRepository = new JdbcClientRepository(database);
        ResourceRequestRepository requestRepository = new JdbcResourceRequestRepository(database);
        ClientService clients = new ClientService(clientRepository, requestRepository);
        ResourceRequestService requests = new ResourceRequestService(requestRepository, clients);
        StatisticsService statistics = new StatisticsService(clients, requests);
        ExportService exports = new ExportService(clients, requests, new ExcelExporter());
        if (args.length > 0) {
            if (args.length != 2 || !args[0].equals("--export")) {
                System.err.println("Использование: java -jar s-cloud-1.0.0.jar [--console | --export файл.xlsx]");
                System.exit(2);
            }
            try {
                Path path = Path.of(args[1]);
                if (!path.toString().endsWith(".xlsx")) throw new IOException("Расширение должно быть .xlsx");
                exports.export(path);
                System.out.println("Экспортировано: " + path.toAbsolutePath());
            } catch (IOException | DataAccessException | InvalidPathException e) {
                System.err.println("Ошибка: " + e.getMessage());
                System.exit(1);
            }
            return;
        }
        try (Scanner scanner = new Scanner(System.in)) {
            new ConsoleUi(scanner, System.out, clients, requests, statistics, exports, database).run();
        }
    }
}
