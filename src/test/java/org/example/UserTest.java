package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Класс для тестирования сущности {@link User}.
 * Содержит юнит-тесты для проверки корректности установки и получения полей,
 * обработки граничных случаев и работы конструкторов.
 */
public class UserTest {
    private static final String TEST_NAME = "John Doe";
    private static final String TEST_EMAIL = "john@example.com";
    private static final int TEST_AGE = 26;
    private static final Long TEST_ID = 25L;

    private User user;

    /**
     * Настраивает окружение перед каждым тестом.
     * Создает новый экземпляр {@link User} для тестирования.
     */
    @BeforeEach
    void setup() {
        user = new User();
    }

    /**
     * Проверяет корректность установки и получения идентификатора пользователя.
     */
    @Test
    @DisplayName("Должен корректно устанавливать и возвращать ID")
    void shouldSetAndGetIdCorrectly() {
        user.setId(TEST_ID);

        assertEquals(TEST_ID, user.getId());
    }

    /**
     * Проверяет обработку null значения для идентификатора пользователя.
     */
    @Test
    @DisplayName("Должен корректно обрабатывать null для ID")
    void shouldHandleNullId() {
        user.setId(null);

        assertNull(user.getId());
    }

    /**
     * Проверяет обработку отрицательного идентификатора пользователя.
     */
    @Test
    @DisplayName("Должен корректно обрабатывать отрицательный ID")
    void shouldHandleNegativeId() {
        user.setId(-1L);

        assertEquals(-1L, user.getId());
    }

    /**
     * Проверяет корректность установки и получения имени пользователя.
     */
    @Test
    @DisplayName("Должен корректно устанавливать и возвращать имя")
    void shouldSetAndGetNameCorrectly() {
        user.setName(TEST_NAME);

        assertEquals(TEST_NAME, user.getName());
    }

    /**
     * Проверяет обработку null значения для имени пользователя.
     */
    @Test
    @DisplayName("Должен корректно обрабатывать null для имени")
    void shouldHandleNullName() {
        user.setName(null);

        assertNull(user.getName());
    }

    /**
     * Проверяет корректность установки и получения email пользователя.
     */
    @Test
    @DisplayName("Должен корректно устанавливать и возвращать email")
    void shouldSetAndGetEmailCorrectly() {
        user.setEmail(TEST_EMAIL);

        assertEquals(TEST_EMAIL, user.getEmail());
    }

    /**
     * Проверяет обработку null значения для email пользователя.
     */
    @Test
    @DisplayName("Должен корректно обрабатывать null для email")
    void shouldHandleNullEmail() {
        user.setEmail(null);

        assertNull(user.getEmail());
    }

    /**
     * Проверяет корректность установки и получения возраста пользователя.
     */
    @Test
    @DisplayName("Должен корректно устанавливать и возвращать возраст")
    void shouldSetAndGetAgeCorrectly() {
        user.setAge(TEST_AGE);

        assertEquals(TEST_AGE, user.getAge());
    }

    /**
     * Проверяет обработку отрицательного возраста пользователя.
     */
    @Test
    @DisplayName("Должен корректно обрабатывать отрицательный возраст")
    void shouldHandleNegativeAge() {
        user.setAge(-1);

        assertEquals(-1, user.getAge());
    }

    /**
     * Проверяет корректность установки и получения времени создания пользователя.
     */
    @Test
    @DisplayName("Должен корректно устанавливать и возвращать createdAt")
    void shouldSetAndGetCreatedAtCorrectly() {
        Timestamp expectedTimestamp = new Timestamp(System.currentTimeMillis());
        user.setCreatedAt(expectedTimestamp);
        Timestamp actualTimestamp = user.getCreatedAt();

        assertEquals(expectedTimestamp, actualTimestamp);
    }

    /**
     * Проверяет, что время создания пользователя по умолчанию равно null.
     */
    @Test
    @DisplayName("Должен корректно обрабатывать null для createdAt по умолчанию")
    void shouldHandleNullCreatedAt() {
        assertNull(user.getCreatedAt());
    }

    /**
     * Проверяет корректность создания пользователя с использованием конструктора по умолчанию.
     */
    @Test
    @DisplayName("Должен корректно создавать пользователя с конструктором по умолчанию")
    void shouldCreateUserWithDefaultConstructor() {
        User testUser = new User();

        assertNull(testUser.getId());
        assertNull(testUser.getName());
        assertNull(testUser.getEmail());
        assertEquals(0, testUser.getAge());
        assertNull(testUser.getCreatedAt());
    }

    /**
     * Проверяет корректность создания пользователя с использованием параметризованного конструктора.
     *
     * @param name  имя пользователя
     * @param email email пользователя
     * @param age   возраст пользователя
     */
    @ParameterizedTest
    @CsvSource({
            "John Doe, john@example.com, 26",
            "Jane Smith, jane@example.com, 17",
            "Alice Brown, alice@example.com, 39",
            "'', '', 30"
    })
    @DisplayName("Должен корректно создавать User с параметризованным конструктором")
    void shouldCreateUserWithValidParameters(String name, String email, int age) {
        User testUser = new User(name, email, age);

        assertEquals(name, testUser.getName());
        assertEquals(email, testUser.getEmail());
        assertEquals(age, testUser.getAge());
    }
}