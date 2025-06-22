package org.example;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Интеграционные тесты для класса {@link UserDaoImpl}, использующие Testcontainers и PostgreSQL.
 */
@ExtendWith(TestEnvironmentExtension.class)
@Testcontainers
class UserDaoImplIntegrationTest {

    private UserDao userDao;
    private ListAppender<ILoggingEvent> listAppender;

    /**
     * Инициализирует DAO и очищает базу данных перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        userDao = new UserDaoImpl();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.createNativeQuery("TRUNCATE TABLE users RESTART IDENTITY", Void.class).executeUpdate();
            session.getTransaction().commit();

        } catch (Exception e) {
            throw new RuntimeException("Failed to clear database", e);
        }

        Logger logger = (Logger) LoggerFactory.getLogger(UserDaoImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    /**
     * Очищает логгер после каждого теста.
     */
    @AfterEach
    void tearDown() {
        listAppender.stop();
        Logger logger = (Logger) LoggerFactory.getLogger(UserDaoImpl.class);
        logger.detachAppender(listAppender);
    }

    /**
     * Тестирует успешное создание пользователя.
     */
    @Test
    @DisplayName("Успешное создание пользователя")
    void testCreateUserSuccess() {
        boolean result = userDao.createUser("John Doe", "john@example.com", 30);

        assertTrue(result);

        User user = userDao.readUser(1L);

        assertNotNull(user);
        assertEquals("John Doe", user.getName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals(30, user.getAge());

        List<ILoggingEvent> logs = listAppender.list;

        assertTrue(logs.stream().anyMatch(event ->
                event.getFormattedMessage().contains("Пользователь создан") &&
                        event.getLevel() == Level.INFO));
    }

    /**
     * Тестирует попытку создания пользователя с уже существующим email.
     */
    @Test
    @DisplayName("Создание пользователя с дублирующимся email")
    void testCreateUserDuplicateEmail() {
        userDao.createUser("John Doe", "john@example.com", 30);
        boolean result = userDao.createUser("Jane Doe", "john@example.com", 25);

        assertFalse(result);

        List<ILoggingEvent> logs = listAppender.list;

        assertTrue(logs.stream().anyMatch(event ->
                event.getFormattedMessage().contains("Ошибка создания пользователя: Email john@example.com уже существует") &&
                        event.getLevel() == Level.ERROR));
    }

    /**
     * Тестирует успешное чтение пользователя по идентификатору.
     */
    @Test
    @DisplayName("Успешное чтение пользователя")
    void testReadUserSuccess() {
        userDao.createUser("John Doe", "john@example.com", 30);
        User user = userDao.readUser(1L);

        assertNotNull(user);
        assertEquals("John Doe", user.getName());

        List<ILoggingEvent> logs = listAppender.list;

        assertTrue(logs.stream().anyMatch(event ->
                event.getFormattedMessage().contains("Пользователь с идентификатором 1 прочитан") &&
                        event.getLevel() == Level.INFO));
    }

    /**
     * Тестирует чтение несуществующего пользователя.
     */
    @Test
    @DisplayName("Чтение несуществующего пользователя")
    void testReadUserNotFound() {
        User user = userDao.readUser(999L);

        assertNull(user);

        List<ILoggingEvent> logs = listAppender.list;

        assertTrue(logs.stream().anyMatch(event ->
                event.getFormattedMessage().contains("Невозможно найти пользователя с идентификатором 999") &&
                        event.getLevel() == Level.INFO));
    }

    /**
     * Тестирует чтение списка всех пользователей.
     */
    @Test
    @DisplayName("Чтение всех пользователей")
    void testReadAllUsers() {
        userDao.createUser("John Doe", "john@example.com", 30);
        userDao.createUser("Jane Doe", "jane@example.com", 25);
        List<User> users = userDao.readAllUsers();

        assertEquals(2, users.size());
        assertEquals("John Doe", users.get(0).getName());
        assertEquals("Jane Doe", users.get(1).getName());

        List<ILoggingEvent> logs = listAppender.list;

        assertTrue(logs.stream().anyMatch(event ->
                event.getFormattedMessage().contains("Список пользователей прочитан") &&
                        event.getLevel() == Level.INFO));
    }

    /**
     * Тестирует успешное обновление пользователя.
     */
    @Test
    @DisplayName("Успешное обновление пользователя")
    void testUpdateUserSuccess() {
        userDao.createUser("John Doe", "john@example.com", 30);
        boolean result = userDao.updateUser(1L, "John Updated", "john.updated@example.com", 31);

        assertTrue(result);

        User user = userDao.readUser(1L);

        assertEquals("John Updated", user.getName());
        assertEquals("john.updated@example.com", user.getEmail());
        assertEquals(31, user.getAge());

        List<ILoggingEvent> logs = listAppender.list;

        assertTrue(logs.stream().anyMatch(event ->
                event.getFormattedMessage().contains("Пользователь с идентификатором 1 обновлен") &&
                        event.getLevel() == Level.INFO));
    }

    /**
     * Тестирует успешное удаление пользователя.
     */
    @Test
    @DisplayName("Успешное удаление пользователя")
    void testRemoveUserSuccess() {
        userDao.createUser("John Doe", "john@example.com", 30);
        boolean result = userDao.removeUser(1L);

        assertTrue(result);

        User user = userDao.readUser(1L);

        assertNull(user);

        List<ILoggingEvent> logs = listAppender.list;

        assertTrue(logs.stream().anyMatch(event ->
                event.getFormattedMessage().contains("Пользователь с идентификатором 1 удален") &&
                        event.getLevel() == Level.INFO));
    }

    /**
     * Тестирует попытку удаления несуществующего пользователя.
     */
    @Test
    @DisplayName("Удаление несуществующего пользователя")
    void testRemoveUserNotFound() {
        boolean result = userDao.removeUser(999L);

        assertFalse(result);

        List<ILoggingEvent> logs = listAppender.list;

        assertTrue(logs.stream().anyMatch(event ->
                event.getFormattedMessage().contains("Пользователь с таким идентификатором: 999 не существует") &&
                        event.getLevel() == Level.ERROR));
    }
}