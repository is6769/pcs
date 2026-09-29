package ru.mirea.cloud.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.cloud.model.*;
import ru.mirea.cloud.service.StatisticsService;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class ExcelExporter {
    public void export(Path path, List<Client> clients, List<ResourceRequest> requests) throws IOException {
        Path absolute = path.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle heading = workbook.createCellStyle();
            heading.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            heading.setFont(font);
            CellStyle date = workbook.createCellStyle();
            date.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setWrapText(true);
            textStyle.setVerticalAlignment(VerticalAlignment.TOP);

            Sheet summary = sheet(workbook, "Статистика", heading, "Показатель", "Значение");
            StatisticsService.calculate(clients, requests).forEach((label, value) -> {
                Row row = summary.createRow(summary.getLastRowNum() + 1);
                row.createCell(0).setCellValue(label);
                row.createCell(1).setCellValue(value);
            });
            finish(summary, 2);

            Sheet customerSheet = sheet(workbook, "Клиенты", heading, "ID", "Имя", "Email", "Компания");
            for (Client client : clients) {
                Row row = customerSheet.createRow(customerSheet.getLastRowNum() + 1);
                values(row, client.getId(), client.getName(), client.getEmail(), client.getCompany());
            }
            finish(customerSheet, 4);

            Sheet requestSheet = sheet(workbook, "Заявки", heading, "ID", "ID клиента", "Название", "Описание",
                    "Тип ресурса", "Регион", "CPU, ядер", "RAM, ГБ", "Диск, ГБ", "Статус", "Создана");
            for (ResourceRequest request : requests) {
                Row row = requestSheet.createRow(requestSheet.getLastRowNum() + 1);
                values(row, request.getId(), request.getClientId(), request.getTitle(), request.getDescription(),
                        request.getResourceType().name(), request.getRegion().name(), request.getCpu(),
                        request.getRamGb(), request.getStorageGb(), request.getStatus().name());
                Cell timestamp = row.createCell(10);
                timestamp.setCellValue(request.getCreatedAt());
                timestamp.setCellStyle(date);
                row.getCell(2).setCellStyle(textStyle);
                row.getCell(3).setCellStyle(textStyle);
                // Allow long descriptions to wrap without silently losing exported content.
                int lines = Math.max(2, (int) Math.ceil(request.getDescription().length() / 48.0));
                row.setHeightInPoints(Math.min(409, lines * 16));
            }
            finish(requestSheet, 11);
            requestSheet.setColumnWidth(2, 36 * 256);
            requestSheet.setColumnWidth(3, 56 * 256);
            requestSheet.setColumnWidth(10, 23 * 256);
            try (OutputStream output = Files.newOutputStream(absolute, StandardOpenOption.CREATE_NEW)) {
                workbook.write(output);
            }
        }
    }

    private static Sheet sheet(Workbook workbook, String name, CellStyle heading, String... labels) {
        Sheet sheet = workbook.createSheet(name);
        Row row = sheet.createRow(0);
        row.setHeightInPoints(26);
        for (int i = 0; i < labels.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(labels[i]);
            cell.setCellStyle(heading);
        }
        sheet.createFreezePane(0, 1);
        return sheet;
    }

    private static void values(Row row, Object... values) {
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            if (values[i] instanceof Number number) cell.setCellValue(number.doubleValue());
            else cell.setCellValue(values[i].toString());
        }
    }

    private static void finish(Sheet sheet, int columns) {
        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, columns - 1));
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(60 * 256, Math.max(12 * 256, sheet.getColumnWidth(i) + 768)));
        }
    }
}
