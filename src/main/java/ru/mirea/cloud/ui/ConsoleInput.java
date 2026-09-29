package ru.mirea.cloud.ui;

import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class ConsoleInput {
    private final Scanner scanner;
    private final PrintStream out;

    public ConsoleInput(Scanner scanner, PrintStream out) { this.scanner = scanner; this.out = out; }

    public String text(String prompt) {
        out.print(prompt + ": ");
        if (!scanner.hasNextLine()) throw new NoSuchElementException("Конец ввода");
        return scanner.nextLine().strip();
    }

    public int number(String prompt) {
        while (true) {
            try { return Integer.parseInt(text(prompt)); }
            catch (NumberFormatException e) { out.println("Ошибка: введите целое число в диапазоне int."); }
        }
    }

    public long id() {
        while (true) {
            try {
                long value = Long.parseLong(text("Введите ID"));
                if (value > 0) return value;
                out.println("Ошибка: ID должен быть положительным.");
            } catch (NumberFormatException e) { out.println("Ошибка: ID должен быть целым числом в диапазоне long."); }
        }
    }

    public <E extends Enum<E>> E choice(String prompt, Class<E> type) {
        E[] options = type.getEnumConstants();
        for (int i = 0; i < options.length; i++) out.printf("%d. %s%n", i + 1, options[i]);
        while (true) {
            int index = number(prompt);
            if (index >= 1 && index <= options.length) return options[index - 1];
            out.println("Ошибка: выберите номер из списка.");
        }
    }
}
