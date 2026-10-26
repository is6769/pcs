package ru.mirea.cloud.service;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.cloud.model.*;
import ru.mirea.cloud.util.ExcelExporter;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {
    @TempDir Path temporary;

    private static ResourceRequest request(long id, String title) {
        return new ResourceRequest(id, 7, title, "Описание", ResourceType.VIRTUAL_MACHINE, Region.RU_MOSCOW,
                2, 4, 100, RequestStatus.NEW, LocalDateTime.of(2026, 10, 26, 12, 0), 0);
    }

    @Test void exportsExactlyTheDisplayedSnapshotAndSortOrderWithoutDatabaseAccess() throws Exception {
        // Null services deliberately make any unexpected database query fail this test.
        ExportService service = new ExportService(null, null, new ExcelExporter());
        Path file = temporary.resolve("filtered.xlsx");
        service.export(file, List.of(new Client(7, "Анна", "anna@example.com", "Компания")),
                List.of(request(19, "Ядро"), request(3, "Архив")));
        try (var input = Files.newInputStream(file); var workbook = new XSSFWorkbook(input)) {
            var requests = workbook.getSheet("Заявки");
            assertEquals(2, requests.getLastRowNum());
            assertEquals(19, requests.getRow(1).getCell(0).getNumericCellValue());
            assertEquals("Ядро", requests.getRow(1).getCell(2).getStringCellValue());
            assertEquals(3, requests.getRow(2).getCell(0).getNumericCellValue());
            assertEquals(CellType.NUMERIC, requests.getRow(1).getCell(10).getCellType());
            assertEquals(1, workbook.getSheet("Клиенты").getLastRowNum());
            assertEquals(2, workbook.getSheet("Статистика").getRow(2).getCell(1).getNumericCellValue());
        }
    }

    @Test void emptyFilterProducesAValidWorkbookAndExistingFilesArePreserved() throws Exception {
        ExportService service = new ExportService(null, null, new ExcelExporter());
        Path file = temporary.resolve("empty.xlsx");
        service.export(file, List.of(), List.of());
        byte[] original = Files.readAllBytes(file);
        try (var input = Files.newInputStream(file); var workbook = new XSSFWorkbook(input)) {
            assertEquals(0, workbook.getSheet("Заявки").getLastRowNum());
            assertEquals(0, workbook.getSheet("Клиенты").getLastRowNum());
            assertEquals(0, workbook.getSheet("Статистика").getRow(2).getCell(1).getNumericCellValue());
        }
        assertThrows(FileAlreadyExistsException.class,
                () -> service.export(file, List.of(), List.of(request(1, "Новая"))));
        assertArrayEquals(original, Files.readAllBytes(file));
    }
}
