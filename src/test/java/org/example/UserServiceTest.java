package org.example;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Класс для тестирования сервиса управления пользователями {@link UserService}.
 * Содержит юнит-тесты для проверки функциональности создания, чтения, обновления и удаления пользователей,
 * а также обработки ошибок и граничных случаев с использованием мок-объектов.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    private static final String INCORRECT_DATA_MESSAGE = "Некорректные данные.";
    private static final String DATABASE_ERROR_MESSAGE = "Произошла ошибка базы данных. Попробуйте еще раз позже.";
    private static final String INVALID_ID_OR_AGE_MESSAGE = "Введите допустимое целое число для идентификатора или возраста.";
    private static final String INCORRECT_ID_MESSAGE = "Некорректный идентификатор.";

    private ListAppender<ILoggingEvent> listAppender;

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserService userService;

    /**
     * Настраивает окружение перед каждым тестом.
     * Инициализирует логгер и добавляет к нему {@link ListAppender} для захвата логов.
     */
    @BeforeEach
    void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(UserService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        listAppender.list.clear();
    }

    /**
     * Очищает окружение после каждого теста.
     * Отсоединяет и останавливает {@link ListAppender} от логгера.
     */
    @AfterEach
    void tearDown() {
        try {
            Logger logger = (Logger) LoggerFactory.getLogger(UserService.class);
            logger.detachAppender(listAppender);
        } finally {
            listAppender.stop();
        }
    }

    /**
     * Проверяет успешное создание пользователя с корректными данными.
     */
    @Test
    @DisplayName("Должен корректно создавать пользователя и вывести сообщение")
    void shouldSuccessfullyCreateUser() {
        String name = "John Doe";
        String email = "john.doe@example.com";
        String age = "23";

        when(userDao.createUser(anyString(), anyString(), anyInt())).thenReturn(true);

        String result = userService.create(name, email, age);

        assertEquals("Пользователь успешно создан.", result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться при удачном создании пользователя");

        verify(userDao).createUser(name.trim(), email, 23);
    }

    /**
     * Проверяет создание пользователя с максимальным граничным значением возраста (150 лет).
     */
    @Test
    @DisplayName("Должен создать пользователя для верхнего граничного случая")
    void shouldCreateUserWithUpperBoundaryAge() {
        String name = "John Doe";
        String email = "john.doe@example.com";
        String age = "150";

        when(userDao.createUser(anyString(), anyString(), anyInt())).thenReturn(true);

        String result = userService.create(name, email, age);

        assertEquals("Пользователь успешно создан.", result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться при удачном создании пользователя");

        verify(userDao).createUser(name, email, 150);
    }

    /**
     * Проверяет создание пользователя с минимальным граничным значением возраста (0 лет).
     */
    @Test
    @DisplayName("Должен создать пользователя для нижнего граничного случая")
    void shouldCreateUserWithBottomBoundaryAge() {
        String name = "John Doe";
        String email = "john.doe@example.com";
        String age = "0";

        when(userDao.createUser(anyString(), anyString(), anyInt())).thenReturn(true);

        String result = userService.create(name, email, age);

        assertEquals("Пользователь успешно создан.", result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться для успешно созданного пользователя");

        verify(userDao).createUser(name, email, 0);
    }

    /**
     * Проверяет неудачное создание пользователя, когда база данных возвращает false.
     */
    @Test
    @DisplayName("Не должен создать пользователя и вывести сообщение")
    void shouldNotCreateUser() {
        String name = "John Doe";
        String email = "john.doe@example.com";
        String age = "23";

        when(userDao.createUser(anyString(), anyString(), anyInt())).thenReturn(false);

        String result = userService.create(name, email, age);

        assertEquals("Не удалось создать пользователя.", result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться при неудачном создании пользователя");

        verify(userDao).createUser(name.trim(), email, 23);
    }

    /**
     * Проверяет обработку некорректного формата возраста при создании пользователя.
     *
     * @param age некорректное значение возраста
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "23.2",
            "23,0",
            "23i",
            "IV",
            "invalid",
            "",
            " "
    })
    @DisplayName("Должен обработать исключение неверного ввода возраста и вывести сообщение")
    void shouldHandleInvalidAgeWhenCreateUser(String age) {
        String name = "John Doe";
        String email = "john.doe@example.com";

        String result = userService.create(name, email, age);

        assertEquals("Введите допустимое целое число для возраста.", result);
        assertTrue(listAppender.list.isEmpty(), "Логи не должны записываться для невалидного возраста");

        verify(userDao, never()).createUser(anyString(), anyString(), anyInt());
    }

    /**
     * Проверяет обработку исключения базы данных при создании пользователя.
     */
    @Test
    @DisplayName("Должен обработать исключение базы данных при создании пользователя и вернуть сообщение")
    void shouldHandleDatabaseExceptionWhenCreate() {
        String name = "John Doe";
        String email = "john.doe@example.com";
        String age = "23";
        HibernateException exception = new HibernateException("Ошибка базы данных");

        when(userDao.createUser(anyString(), anyString(), anyInt())).thenThrow(exception);

        String result = userService.create(name, email, age);

        assertEquals(DATABASE_ERROR_MESSAGE, result);
        assertEquals(1, listAppender.list.size());
        assertEquals("Ошибка базы данных при создании пользователя: " + exception.getMessage(),
                listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.ERROR, listAppender.list.get(0).getLevel());

        verify(userDao).createUser(name.trim(), email, 23);
    }

    /**
     * Проверяет обработку исключения недопустимого аргумента при создании пользователя.
     */
    @Test
    @DisplayName("Должно обрабатывать исключение недопустимого аргумента и вывести сообщение")
    void shouldHandleIllegalArgumentExceptionWhenCreateUser() {
        String name = "John Doe";
        String email = "john.doe@example.com";
        String age = "23";
        IllegalArgumentException exception = new IllegalArgumentException("Недопустимый аргумент");

        when(userDao.createUser(anyString(), anyString(), anyInt())).thenThrow(exception);

        String result = userService.create(name, email, age);

        assertEquals("Не удалось создать пользователя: " + exception.getMessage(), result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться для исключения недопустимого аргумента");

        verify(userDao).createUser(name.trim(), email, 23);
    }

    /**
     * Проверяет обработку некорректных данных при создании пользователя.
     *
     * @param name        имя пользователя
     * @param email       электронная почта пользователя
     * @param age         возраст пользователя
     * @param description описание тестового случая
     */
    @ParameterizedTest
    @CsvSource({
            "'', john.doe@email.com, 23, # Пустое имя",
            "John Doe, john%doe@email.com, 23, # Неверный формат email",
            "John Doe, john.doe@email.com, 151, # Возраст > 150",
            "John Doe, john.doe@email.com, 2147483646, # Возраст > 150",
            "John Doe, john.doe@email.com, -1, # Возраст < 0",
            "John Doe, john.doe@email.com, -2147483648, # Возраст < 0",
            "John.Doe,  john.doe@email.com, 23, # Неверный формат имени",
            "'', '', 23, # Пустые имя и email",
            "John Doe, '', 23, # Пустой email"
    })
    @DisplayName("Должен обработать некорректные данные и вывести сообщение")
    void shouldHandleIncorrectDataWhenCreateUser(String name, String email, String age, String description) {
        String result = userService.create(name, email, age);

        assertEquals(INCORRECT_DATA_MESSAGE, result,
                "Ошибка для случая: " + description);
        assertTrue(listAppender.list.isEmpty(), "Логи не должны записываться для некорректных данных");

        verify(userDao, never()).createUser(anyString(), anyString(), anyInt());
    }

    /**
     * Проверяет успешное чтение данных пользователя по идентификатору.
     */
    @Test
    @DisplayName("Должен корректно отображать данные пользователя при чтении")
    void shouldReturnReadUser() {
        String id = "1";
        String name = "John Doe";
        String email = "john.doe@example.com";
        User testUser = new User(name, email, 23);
        testUser.setId(1L);
        testUser.setCreatedAt(Timestamp.valueOf("2025-06-21 10:00:00"));

        when(userDao.readUser(anyLong())).thenReturn(testUser);

        String result = userService.read(id);

        String expected = """
                Пользователь с идентификатором 1:
                Создан в: 2025-06-21 10:00:00.0
                Имя: John Doe, \
                email: john.doe@example.com, возраст: 23""";

        assertEquals(expected, result, "Формат вывода данных пользователя не корректен");
        assertTrue(listAppender.list.isEmpty(), "Логи не должны записываться для прочитанного пользователя");

        verify(userDao).readUser(1L);
    }

    /**
     * Проверяет обработку некорректного идентификатора при чтении пользователя.
     *
     * @param id некорректный идентификатор
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "0",
            "-1",
            "-124089435",
            "-9223372036854775808" //Long.MIN_VALUE
    })
    @DisplayName("Должен обработать неверный ID и вывести сообщение")
    void shouldHandleIncorrectIdWhenReadUser(String id) {
        String result = userService.read(id);

        assertEquals(INCORRECT_ID_MESSAGE, result);
        assertTrue(listAppender.list.isEmpty(), "Логи не должны записываться для некорректного ID");

        verify(userDao, never()).readUser(anyLong());
    }

    /**
     * Проверяет обработку невалидного формата идентификатора при чтении пользователя.
     *
     * @param id невалидный идентификатор
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "1L",
            "0L",
            "invalid",
            "0.2",
            "",
            " "
    })
    @DisplayName("Должен обработать исключение невалидного ID и вывести сообщение")
    void shouldHandleInvalidIdWhenReadUser(String id) {
        String result = userService.read(id);

        assertEquals("Введите допустимое целое число для идентификатора.", result);
        assertTrue(listAppender.list.isEmpty(), "Логи не должны записываться для невалидного ID");

        verify(userDao, never()).readUser(anyLong());
    }

    /**
     * Проверяет обработку исключения базы данных при чтении пользователя.
     */
    @Test
    @DisplayName("Должен обработать исключение базы данных при чтении пользователя и вернуть сообщение")
    void shouldHandleDatabaseExceptionWhenReadUser() {
        String id = "1";
        HibernateException exception = new HibernateException("Ошибка базы данных");

        when(userDao.readUser(anyLong())).thenThrow(exception);

        String result = userService.read(id);

        assertEquals(DATABASE_ERROR_MESSAGE, result);
        assertEquals(1, listAppender.list.size());
        assertEquals("Ошибка базы данных при чтении пользователя: " + exception.getMessage(),
                listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.ERROR, listAppender.list.get(0).getLevel());

        verify(userDao).readUser(1L);
    }

    /**
     * Проверяет обработку случая, когда пользователь не найден при чтении.
     */
    @Test
    @DisplayName("Должен вернуть сообщение о ненайденном пользователе, если пользователь не существует")
    void shouldReturnMessageForNullUserWhenRead() {
        String id = "1";

        when(userDao.readUser(anyLong())).thenReturn(null);

        String result = userService.read(id);

        assertEquals("Пользователь не найден.", result);
        assertTrue(listAppender.list.isEmpty(), "Логи не должны записываться, если пользователь не найден");

        verify(userDao).readUser(1L);
    }

    /**
     * Проверяет успешное чтение списка пользователей и форматирование результата.
     */
    @Test
    @DisplayName("Должен вернуть список пользователей")
    void shouldReturnFormattedUserTable() {
        List<User> users = new ArrayList<>();
        User user1 = new User("John Doe", "john.doe@example.com", 23);
        user1.setId(1L);
        user1.setCreatedAt(Timestamp.valueOf("2025-06-21 10:00:00"));
        User user2 = new User("Татьяна", "tatyana@example.com", 33);
        user2.setId(2L);
        user2.setCreatedAt(Timestamp.valueOf("2025-06-21 10:01:00"));
        User user3 = new User("Jake Smith", "jake123@example.com", 18);
        user3.setId(3L);
        user3.setCreatedAt(Timestamp.valueOf("2025-06-21 10:02:00"));
        users.add(user1);
        users.add(user2);
        users.add(user3);

        when(userDao.readAllUsers()).thenReturn(users);

        String result = userService.readAll();

        String[] lines = result.split("\n");

        assertEquals("Пользователи:", lines[0].trim());
        assertTrue(lines[1].contains("ID") && lines[1].contains("Имя") &&
                lines[1].contains("Email") && lines[1].contains("Возраст") &&
                lines[1].contains("Создан"));

        //user1
        assertTrue(lines[2].contains("1") && lines[2].contains("John Doe") &&
                lines[2].contains("john.doe@example.com") && lines[2].contains("23") &&
                lines[2].contains("2025-06-21 10:00:00"));

        //user2
        assertTrue(lines[3].contains("2") && lines[3].contains("Татьяна") &&
                lines[3].contains("tatyana@example.com") && lines[3].contains("33") &&
                lines[3].contains("2025-06-21 10:01:00"));

        //user3
        assertTrue(lines[4].contains("3") && lines[4].contains("Jake Smith") &&
                lines[4].contains("jake123@example.com") && lines[4].contains("18") &&
                lines[4].contains("2025-06-21 10:02:00"));

        assertEquals(5, lines.length, "Некорректное количество строк в результате");
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться для полученного списка пользователей");

        verify(userDao).readAllUsers();
    }

    /**
     * Проверяет обработку случая, когда список пользователей равен null.
     */
    @Test
    @DisplayName("Должен обработать и вывести сообщение для null списка пользователей")
    void shouldHandleNullUserListWhenReadAllUsers() {
        when(userDao.readAllUsers()).thenReturn(null);

        String result = userService.readAll();

        assertEquals("Ошибка чтения пользователей.", result);
        assertEquals(1, listAppender.list.size());
        assertEquals("Не удалось прочитать пользователей из базы данных",
                listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.ERROR, listAppender.list.get(0).getLevel());

        verify(userDao).readAllUsers();
    }

    /**
     * Проверяет обработку пустого списка пользователей.
     */
    @Test
    @DisplayName("Должен обработать и вывести сообщение для пустого списка пользователей")
    void shouldHandleEmptyUserListWhenReadAllUsers() {
        when(userDao.readAllUsers()).thenReturn(List.of());

        String result = userService.readAll();

        assertEquals("Пользователи не найдены.", result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться для пустого списка пользователей");

        verify(userDao).readAllUsers();
    }

    /**
     * Проверяет успешное обновление пользователя с корректными данными.
     */
    @Test
    @DisplayName("Должен корректно обновлять пользователя и выводить сообщение")
    void shouldCorrectlyUpdateUser() {
        String id = "1";
        String newName = "John Doe";
        String newEmail = "john.doe@example.com";
        String newAge = "23";

        when(userDao.updateUser(anyLong(), anyString(), anyString(), anyInt())).thenReturn(true);

        String result = userService.update(id, newName, newEmail, newAge);

        assertEquals("Пользователь обновлен успешно.", result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться при удачном обновлении пользователя");

        verify(userDao).updateUser(1L, newName, newEmail, 23);
    }

    /**
     * Проверяет обновление пользователя с максимальным граничным значением возраста (150 лет).
     */
    @Test
    @DisplayName("Должен корректно обновлять пользователя с верхним граничным возрастом")
    void shouldUpdateUserWithUpperBoundaryAge() {
        String id = "1";
        String newName = "John Doe";
        String newEmail = "john.doe@example.com";
        String newAge = "150";

        when(userDao.updateUser(anyLong(), anyString(), anyString(), anyInt())).thenReturn(true);

        String result = userService.update(id, newName, newEmail, newAge);

        assertEquals("Пользователь обновлен успешно.", result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться при удачном обновлении пользователя");

        verify(userDao).updateUser(1L, newName, newEmail, 150);
    }

    /**
     * Проверяет обновление пользователя с минимальным граничным значением возраста (0 лет).
     */
    @Test
    @DisplayName("Должен корректно обновлять пользователя с нижним граничным возрастом")
    void shouldUpdateUserWithBottomBoundaryAge() {
        String id = "1";
        String newName = "John Doe";
        String newEmail = "john.doe@example.com";
        String newAge = "0";

        when(userDao.updateUser(anyLong(), anyString(), anyString(), anyInt())).thenReturn(true);

        String result = userService.update(id, newName, newEmail, newAge);

        assertEquals("Пользователь обновлен успешно.", result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться при удачном обновлении пользователя");

        verify(userDao).updateUser(1L, newName, newEmail, 0);
    }

    /**
     * Проверяет неудачное обновление пользователя, когда база данных возвращает false.
     */
    @Test
    @DisplayName("Не должен обновлять пользователя и выводит сообщение")
    void shouldNotUpdateUser() {
        String id = "1";
        String newName = "John Doe";
        String newEmail = "john.doe@example.com";
        String newAge = "23";

        when(userDao.updateUser(anyLong(), anyString(), anyString(), anyInt())).thenReturn(false);

        String result = userService.update(id, newName, newEmail, newAge);

        assertEquals("Не удалось обновить пользователя. " +
                        "Пользователь не найден или адрес электронной почты уже существует.",
                result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться при неудачном обновлении пользователя");

        verify(userDao).updateUser(1L, newName, newEmail, 23);
    }

    /**
     * Проверяет обработку некорректных данных при обновлении пользователя.
     *
     * @param id          идентификатор пользователя
     * @param newName     новое имя пользователя
     * @param newEmail    новый email пользователя
     * @param newAge      новый возраст пользователя
     * @param description описание тестового случая
     */
    @ParameterizedTest
    @CsvSource({
            "0, John Doe, john.doe@example.com, 23, # id = 0",
            "-1, John Doe, john.doe@exampl.com, 23, # id < 0",
            "-9223372036854775808, John Doe, john.doe@example.com, 23, # id < 0",
            "1, John.Doe, john.doe@example.com, 23, # Неверный формат у имени",
            "1, John Doe, john/doe@example. com, 23, # Неверный формат у email",
            "1, John Doe, john.doe@example.com, -1, # Возраст < 0",
            "1, John Doe, john.doe@example.com, 151, # Возраст > 150",
            "1, John Doe, john.doe@example.com, 2147483647, # Возраст > 150",
            "1, John Doe, john.doe@example.com, -2147483648, # Возраст < 0",
            "1, '', john.doe@example.com, 23, # Пустое имя",
            "1, John Doe, '', 23, # Пустой email"
    })
    @DisplayName("Должен обработать некорректные данные и вывести сообщение")
    void shouldHandleIncorrectDataWhenUpdateUser(String id, String newName, String newEmail, String newAge, String description) {
        String result = userService.update(id, newName, newEmail, newAge);

        assertEquals(INCORRECT_DATA_MESSAGE, result,
                "Ошибка для случая: " + description);
        assertTrue(listAppender.list.isEmpty(), "Логи не должны вызываться для некорректных данных");

        verify(userDao, never()).updateUser(anyLong(), anyString(), anyString(), anyInt());
    }

    /**
     * Проверяет обработку невалидного формата идентификатора при обновлении пользователя.
     *
     * @param id невалидный идентификатор
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "1L",
            "0L",
            "invalid",
            "0.2",
            "",
            " "
    })
    @DisplayName("Должен обработать невалидный формат ID при обновлении пользователя и вернуть сообщение")
    void shouldHandleIncorrectIdWhenUpdateUser(String id) {
        String newName = "John Doe";
        String newEmail = "john.doe@example.com";
        String newAge = "23";

        String result = userService.update(id, newName, newEmail, newAge);

        assertEquals(INVALID_ID_OR_AGE_MESSAGE, result);
        assertTrue(listAppender.list.isEmpty(), "Логи не должны записываться для невалидного ID");

        verify(userDao, never()).updateUser(anyLong(), anyString(), anyString(), anyInt());
    }

    /**
     * Проверяет обработку невалидного формата возраста при обновлении пользователя.
     *
     * @param newAge невалидный возраст
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "23i",
            "IV",
            "invalid",
            "24.2",
            "24,2",
            "",
            " "
    })
    @DisplayName("Должен обработать невалидный формат возраста при обновлении пользователя и вернуть сообщение")
    void shouldHandleIncorrectAgeWhenUpdateUser(String newAge) {
        String id = "1";
        String newName = "John Doe";
        String newEmail = "john.doe@example.com";

        String result = userService.update(id, newName, newEmail, newAge);

        assertEquals(INVALID_ID_OR_AGE_MESSAGE, result);
        assertTrue(listAppender.list.isEmpty(), "Логи не должны записываться для невалидного возраста");

        verify(userDao, never()).updateUser(anyLong(), anyString(), anyString(), anyInt());
    }

    /**
     * Проверяет обработку исключения недопустимого аргумента при обновлении пользователя.
     */
    @Test
    @DisplayName("Должен обработать исключение недопустимого аргумента и вывести сообщение")
    void shouldHandleIllegalArgumentExceptionWhenUpdateUser() {
        String id = "1";
        String newName = "John Doe";
        String newEmail = "john.doe@example.com";
        String newAge = "23";
        IllegalArgumentException exception = new IllegalArgumentException("Недопустимый аргумент");

        when(userDao.updateUser(anyLong(), anyString(), anyString(), anyInt())).thenThrow(exception);

        String result = userService.update(id, newName, newEmail, newAge);

        assertEquals("Не удалось обновить пользователя: " + exception.getMessage(), result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться для обработки исключения недопустимого аргумента");

        verify(userDao).updateUser(1L, newName, newEmail, 23);
    }

    /**
     * Проверяет обработку исключения базы данных при обновлении пользователя.
     */
    @Test
    @DisplayName("Должен обработать исключение базы данных при обновлении пользователя и вернуть сообщение")
    void shouldHandleHibernateExceptionWhenUpdateUser() {
        String id = "1";
        String newName = "John Doe";
        String newEmail = "john.doe@example.com";
        String newAge = "23";
        HibernateException exception = new HibernateException("Ошибка базы данных");

        when(userDao.updateUser(anyLong(), anyString(), anyString(), anyInt())).thenThrow(exception);

        String result = userService.update(id, newName, newEmail, newAge);

        assertEquals(DATABASE_ERROR_MESSAGE, result);
        assertEquals(1, listAppender.list.size());
        assertEquals("Произошла ошибка базы данных при обновлении пользователя: " + exception.getMessage(),
                listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.ERROR, listAppender.list.get(0).getLevel());

        verify(userDao).updateUser(1L, newName, newEmail, 23);
    }

    /**
     * Проверяет успешное удаление пользователя.
     */
    @Test
    @DisplayName("Должен успешно удалять пользователя и выводит сообщение")
    void shouldSuccessfullyDeleteUser() {
        String id = "1";

        when(userDao.removeUser(anyLong())).thenReturn(true);

        String result = userService.delete(id);

        assertEquals("Пользователь успешно удален.", result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться для успешного удаления пользователя");

        verify(userDao).removeUser(1L);
    }

    /**
     * Проверяет неудачное удаление пользователя, когда база данных возвращает false.
     */
    @Test
    @DisplayName("Не должен удалять пользователя и выводит сообщение")
    void shouldNotDeleteUser() {
        String id = "1";

        when(userDao.removeUser(anyLong())).thenReturn(false);

        String result = userService.delete(id);

        assertEquals("Не удалось удалить пользователя. Пользователь не найден.", result);
        assertTrue(listAppender.list.isEmpty(),
                "Логи не должны записываться для неуспешного удаления пользователя");

        verify(userDao).removeUser(1L);
    }

    /**
     * Проверяет обработку некорректного идентификатора при удалении пользователя.
     *
     * @param id некорректный идентификатор
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "0",
            "-1",
            "-9223372036854775808" //Long.MIN_VALUE
    })
    @DisplayName("Должен обработать неверный ID и вывести сообщение")
    void shouldHandleIncorrectIdWhenDeleteUser(String id) {
        String result = userService.delete(id);

        assertEquals(INCORRECT_ID_MESSAGE, result);
        assertTrue(listAppender.list.isEmpty(), "Логи не должны вызываться для некорректного ID");

        verify(userDao, never()).removeUser(anyLong());
    }

    /**
     * Проверяет обработку невалидного формата идентификатора при удалении пользователя.
     *
     * @param id невалидный идентификатор
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "1L",
            "2.2",
            "2.0",
            "2,0",
            "invalid",
            "",
            " "
    })
    @DisplayName("Должен обработать невалидный формат ID при удалении пользователя и вернуть сообщение")
    void shouldHandleInvalidIdWhenDeleteUser(String id) {
        String result = userService.delete(id);

        assertEquals("Введите допустимое целое число для идентификатора.", result);
        assertTrue(listAppender.list.isEmpty(), "Логи не должны записываться для невалидного ID");

        verify(userDao, never()).removeUser(anyLong());
    }

    /**
     * Проверяет обработку исключения базы данных при удалении пользователя.
     */
    @Test
    @DisplayName("Должен обработать исключение базы данных при удалении пользователя и вернуть сообщение")
    void shouldHandleHibernateExceptionWhenDeleteUser() {
        String id = "1";
        HibernateException exception = new HibernateException("Ошибка базы данных");

        when(userDao.removeUser(anyLong())).thenThrow(exception);

        String result = userService.delete(id);

        assertEquals(DATABASE_ERROR_MESSAGE, result);
        assertEquals(1, listAppender.list.size());
        assertEquals("Произошла ошибка базы данных при удалении пользователя: " + exception.getMessage(),
                listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.ERROR, listAppender.list.get(0).getLevel());

        verify(userDao).removeUser(1L);
    }
}