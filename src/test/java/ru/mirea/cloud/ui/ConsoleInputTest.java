package ru.mirea.cloud.ui;

import org.junit.jupiter.api.Test;
import ru.mirea.cloud.model.RequestStatus;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleInputTest {
    @Test void recoversFromInvalidNumbersAndEnums() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ConsoleInput input = new ConsoleInput(new Scanner("abc\n999999999999\n3\nx\n-1\n9223372036854775807\n9\n2\n"), new PrintStream(buffer));
        assertEquals(3, input.number("Число"));
        assertEquals(Long.MAX_VALUE, input.id());
        assertEquals(RequestStatus.APPROVED, input.choice("Статус", RequestStatus.class));
        assertTrue(buffer.toString().contains("Ошибка"));
        assertThrows(NoSuchElementException.class, () -> input.text("EOF"));
    }
}
