package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Класс для тестирования пользовательского консольного интерфейса {@link UserConsoleUI}.
 * Содержит юнит-тесты для проверки функциональности меню, ввода данных и взаимодействия
 * с сервисом {@link UserService} с использованием мок-объектов.
 */
@ExtendWith(MockitoExtension.class)
class UserConsoleUITest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserConsoleUI userConsoleUI;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;

    private static final String MENU_PROMPT = """
            Меню:
            ==============================
            1. Создать пользователя
            2. Прочитать пользователя
            3. Прочитать всех пользователей
            4. Обновить пользователя
            5. Удалить пользователя
            6. Выйти из приложения
            Выберите действие: """;
    private static final String ENTER_PROMPT = "Нажмите Enter, чтобы продолжить...";
    private static final String INVALID_INPUT = "Неправильный ввод, используйте цифры от 1 до 6.";
    private static final String EXIT_MESSAGE = "Приложение завершено.";

    /**
     * Настраивает окружение перед каждым тестом.
     * Перенаправляет стандартный вывод в {@link ByteArrayOutputStream} и очищает его.
     */
    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        outContent.reset();
    }

    /**
     * Восстанавливает стандартные потоки ввода-вывода после каждого теста.
     */
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    /**
     * Устанавливает пользовательский ввод для теста.
     *
     * @param input строка, имитирующая ввод пользователя
     */
    private void setInput(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        userConsoleUI = new UserConsoleUI(userService, new Scanner(System.in));
    }

    /**
     * Нормализует вывод, удаляя различия в переносах строк и лишние пробелы.
     *
     * @param output строка вывода для нормализации
     * @return нормализованная строка
     */
    private String normalizeOutput(String output) {
        return output.replaceAll("\r\n", "\n").trim();
    }

    /**
     * Тестирует отображение меню и немедленный выход из приложения при выборе опции 6.
     */
    @Test
    @DisplayName("Проверка отображения меню и немедленного выхода")
    void testStartShowMenuAndExit() {
        setInput("6\n");

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains(MENU_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));
        verifyNoInteractions(userService);
    }

    /**
     * Тестирует создание пользователя с корректным вводом данных.
     * Проверяет вызов сервиса и отображение соответствующего сообщения.
     */
    @Test
    @DisplayName("Проверка создания пользователя с корректным вводом")
    void testStartCreateActionValidInput() {
        String input = """
                1
                John Doe
                john@example.com
                30
                
                6
                """;
        setInput(input);

        when(userService.create("John Doe", "john@example.com", "30"))
                .thenReturn("Пользователь создан успешно.");

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains("Введите имя:"));
        assertTrue(output.contains("Введите email:"));
        assertTrue(output.contains("Введите возраст:"));
        assertTrue(output.contains("Пользователь создан успешно."));
        assertTrue(output.contains(ENTER_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));

        verify(userService).create("John Doe", "john@example.com", "30");
        verifyNoMoreInteractions(userService);
    }

    /**
     * Тестирует создание пользователя с пустым именем.
     * Проверяет обработку ошибки и отображение сообщения об ошибке.
     */
    @Test
    @DisplayName("Проверка создания пользователя с пустым вводом")
    void testStartCreateActionEmptyInput() {
        String input = """
                1
                
                john@example.com
                30
                
                6
                """;
        setInput(input);

        when(userService.create("", "john@example.com", "30"))
                .thenReturn("Ошибка: имя не может быть пустым.");

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains("Введите имя:"));
        assertTrue(output.contains("Ошибка: имя не может быть пустым."));
        assertTrue(output.contains(ENTER_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));

        verify(userService).create("", "john@example.com", "30");
        verifyNoMoreInteractions(userService);
    }

    /**
     * Тестирует создание пользователя с некорректным возрастом.
     * Проверяет обработку исключения и отображение сообщения об ошибке.
     */
    @Test
    @DisplayName("Проверка создания пользователя с некорректным возрастом")
    void testStartCreateActionInvalidAge() {
        String input = """
                1
                John Doe
                john@example.com
                abc
                
                6
                """;
        setInput(input);

        when(userService.create("John Doe", "john@example.com", "abc"))
                .thenThrow(new RuntimeException("Некорректный формат возраста"));

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains("Введите имя:"));
        assertTrue(output.contains("Введите email:"));
        assertTrue(output.contains("Введите возраст:"));
        assertTrue(output.contains("Произошла ошибка: Некорректный формат возраста"));
        assertTrue(output.contains(ENTER_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));

        verify(userService).create("John Doe", "john@example.com", "abc");
        verifyNoMoreInteractions(userService);
    }

    /**
     * Тестирует чтение пользователя по идентификатору с корректным вводом.
     * Проверяет вызов сервиса и отображение данных пользователя.
     */
    @Test
    @DisplayName("Проверка чтения пользователя с корректным вводом")
    void testStartReadActionValidInput() {
        String input = """
                2
                1
                
                6
                """;
        setInput(input);

        when(userService.read("1")).thenReturn("Пользователь: John Doe");

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains("Введите идентификатор пользователя:"));
        assertTrue(output.contains("Пользователь: John Doe"));
        assertTrue(output.contains(ENTER_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));

        verify(userService).read("1");
        verifyNoMoreInteractions(userService);
    }

    /**
     * Тестирует чтение всех пользователей.
     * Проверяет вызов сервиса и отображение списка пользователей.
     */
    @Test
    @DisplayName("Проверка чтения всех пользователей")
    void testStartReadAllAction() {
        String input = """
                3
                
                6
                """;
        setInput(input);

        when(userService.readAll()).thenReturn("Все пользователи: []");

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains("Все пользователи: []"));
        assertTrue(output.contains(ENTER_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));

        verify(userService).readAll();
        verifyNoMoreInteractions(userService);
    }

    /**
     * Тестирует обновление пользователя с корректным вводом.
     * Проверяет вызов сервиса и отображение сообщения об успешном обновлении.
     */
    @Test
    @DisplayName("Проверка обновления пользователя с корректным вводом")
    void testStartUpdateActionValidInput() {
        String input = """
                4
                1
                Jane Doe
                jane@example.com
                25
                
                6
                """;
        setInput(input);

        when(userService.update("1", "Jane Doe", "jane@example.com", "25"))
                .thenReturn("Пользователь успешно обновлен");

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains("Введите идентификатор пользователя:"));
        assertTrue(output.contains("Введите новое имя:"));
        assertTrue(output.contains("Введите новый email:"));
        assertTrue(output.contains("Введите новый возраст:"));
        assertTrue(output.contains("Пользователь успешно обновлен"));
        assertTrue(output.contains(ENTER_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));

        verify(userService).update("1", "Jane Doe", "jane@example.com", "25");
        verifyNoMoreInteractions(userService);
    }

    /**
     * Тестирует удаление пользователя с корректным вводом.
     * Проверяет вызов сервиса и отображение сообщения об успешном удалении.
     */
    @Test
    @DisplayName("Проверка удаления пользователя с корректным вводом")
    void testStartDeleteActionValidInput() {
        String input = """
                5
                1
                
                6
                """;
        setInput(input);

        when(userService.delete("1")).thenReturn("Пользователь успешно удален");

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains("Введите идентификатор пользователя:"));
        assertTrue(output.contains("Пользователь успешно удален"));
        assertTrue(output.contains(ENTER_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));

        verify(userService).delete("1");
        verifyNoMoreInteractions(userService);
    }

    /**
     * Тестирует обработку многократного некорректного ввода в меню.
     * Проверяет отображение сообщения об ошибке и корректное завершение.
     */
    @Test
    @DisplayName("Проверка обработки многократного некорректного ввода")
    void testStartMultipleInvalidInputs() {
        String input = """
                7
                
                abc
                
                6
                """;
        setInput(input);

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains(INVALID_INPUT));
        assertEquals(2, output.split(INVALID_INPUT).length - 1); // Два некорректных ввода
        assertTrue(output.contains(ENTER_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));

        verifyNoInteractions(userService);
    }

    /**
     * Тестирует обработку исключения при чтении пользователя с некорректным идентификатором.
     * Проверяет отображение сообщения об ошибке.
     */
    @Test
    @DisplayName("Проверка обработки исключений при чтении пользователя")
    void testStartReadActionException() {
        String input = """
                2
                invalid_id
                
                6
                """;
        setInput(input);

        when(userService.read("invalid_id"))
                .thenThrow(new RuntimeException("Пользователь не найден"));

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains("Введите идентификатор пользователя:"));
        assertTrue(output.contains("Произошла ошибка: Пользователь не найден"));
        assertTrue(output.contains(ENTER_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));

        verify(userService).read("invalid_id");
        verifyNoMoreInteractions(userService);
    }

    /**
     * Тестирует обработку исключения при обновлении пользователя с некорректным идентификатором.
     * Проверяет отображение сообщения об ошибке.
     */
    @Test
    @DisplayName("Проверка обработки исключений при обновлении пользователя")
    void testStartUpdateActionException() {
        String input = """
                4
                invalid_id
                Jane Doe
                jane@example.com
                25
                
                6
                """;
        setInput(input);

        when(userService.update("invalid_id", "Jane Doe", "jane@example.com", "25"))
                .thenThrow(new RuntimeException("Пользователь не найден"));

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains("Введите идентификатор пользователя:"));
        assertTrue(output.contains("Произошла ошибка: Пользователь не найден"));
        assertTrue(output.contains(ENTER_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));

        verify(userService).update("invalid_id", "Jane Doe", "jane@example.com", "25");
        verifyNoMoreInteractions(userService);
    }

    /**
     * Тестирует обработку исключения при чтении всех пользователей.
     * Проверяет отображение сообщения об ошибке.
     */
    @Test
    @DisplayName("Проверка обработки исключений при чтении всех пользователей")
    void testStartReadAllActionException() {
        String input = """
                3
                
                6
                """;
        setInput(input);

        when(userService.readAll()).thenThrow(new RuntimeException("Пользователи не найдены"));

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains("Произошла ошибка: Пользователи не найдены"));
        assertTrue(output.contains(ENTER_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));

        verify(userService).readAll();
        verifyNoMoreInteractions(userService);
    }

    /**
     * Тестирует обработку исключения при удалении пользователя с некорректным идентификатором.
     * Проверяет отображение сообщения об ошибке.
     */
    @Test
    @DisplayName("Проверка обработки исключений при удалении пользователя")
    void testStartDeleteActionException() {
        String input = """
                5
                1
                
                6
                """;
        setInput(input);

        when(userService.delete("1")).thenThrow(new RuntimeException("Пользователь не найден"));

        userConsoleUI.start();
        String output = normalizeOutput(outContent.toString());

        assertTrue(output.contains("Введите идентификатор пользователя:"));
        assertTrue(output.contains("Произошла ошибка: Пользователь не найден"));
        assertTrue(output.contains(ENTER_PROMPT));
        assertTrue(output.contains(EXIT_MESSAGE));

        verify(userService).delete("1");
        verifyNoMoreInteractions(userService);
    }
}