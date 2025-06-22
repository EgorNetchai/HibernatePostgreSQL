package org.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Класс для тестирования утилиты валидации пользователей {@link UserValidationUtil}.
 * Содержит юнит-тесты для проверки корректности методов валидации имени, email, возраста и идентификатора,
 * а также обработки существующих email и граничных случаев.
 */
@ExtendWith(MockitoExtension.class)
public class UserValidationUtilTest {
    private final int MIN_AGE = 0;
    private final int MAX_AGE = 150;
    private final long MIN_ID = 1;

    private ByteArrayOutputStream outContent;
    private ListAppender<ILoggingEvent> listAppender;

    @Mock
    private Session session;

    @Mock
    private SelectionQuery<User> selectionQuery;

    /**
     * Настраивает окружение перед каждым тестом.
     * Инициализирует поток вывода и логгер с {@link ListAppender} для захвата логов.
     */
    @BeforeEach
    void setup() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        Logger logger = (Logger) LoggerFactory.getLogger(UserValidationUtil.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        listAppender.list.clear();
        outContent.reset();
    }

    /**
     * Очищает окружение после каждого теста.
     * Отсоединяет и останавливает {@link ListAppender} от логгера.
     */
    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(UserValidationUtil.class);
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    /**
     * Проверяет выброс исключения при попытке использовать уже существующий email.
     */
    @Test
    @DisplayName("Должен выбросить исключение для уже существующего email")
    void shouldThrowExceptionForExistingEmail() {
        String email = "john@example.com";

        when(session.createSelectionQuery(anyString(), eq(User.class))).thenReturn(selectionQuery);
        when(selectionQuery.setParameter("email", email)).thenReturn(selectionQuery);
        when(selectionQuery.uniqueResult()).thenReturn(new User());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> UserValidationUtil.checkEmailExists(session, email));

        assertEquals("Email " + email + " уже существует", exception.getMessage());

        assertEquals(1, listAppender.list.size());
        assertEquals("Email " + email + " уже существует", listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.ERROR, listAppender.list.get(0).getLevel());

        verify(session).createSelectionQuery("FROM User WHERE email = :email", User.class);
        verify(selectionQuery).setParameter("email", email);
        verify(selectionQuery).uniqueResult();
    }

    /**
     * Проверяет отсутствие исключения для несуществующего email.
     */
    @Test
    @DisplayName("Не должен выбрасывать исключение для не существующего email")
    void shouldNotThrowExceptionForEmailThatDoesNotExist() {
        String email = "john@example.com";

        when(session.createSelectionQuery(anyString(), eq(User.class))).thenReturn(selectionQuery);
        when(selectionQuery.setParameter("email", email)).thenReturn(selectionQuery);
        when(selectionQuery.uniqueResult()).thenReturn(null);

        assertDoesNotThrow(() -> UserValidationUtil.checkEmailExists(session, email));

        verify(session).createSelectionQuery("FROM User WHERE email = :email", User.class);
        verify(selectionQuery).setParameter("email", email);
        verify(selectionQuery).uniqueResult();
    }

    /**
     * Проверяет корректную обработку null значения для email.
     */
    @Test
    @DisplayName("Должен корректно обрабатывать null email")
    void shouldHandleNullEmail() {
        when(session.createSelectionQuery(anyString(), eq(User.class))).thenReturn(selectionQuery);
        when(selectionQuery.setParameter("email", null)).thenReturn(selectionQuery);
        when(selectionQuery.uniqueResult()).thenReturn(null);

        assertDoesNotThrow(() -> UserValidationUtil.checkEmailExists(session, null));

        verify(session).createSelectionQuery("FROM User WHERE email = :email", User.class);
        verify(selectionQuery).setParameter("email", null);
        verify(selectionQuery).uniqueResult();
    }

    /**
     * Проверяет валидность корректных имен пользователей.
     *
     * @param name имя для проверки
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "John Doe",
            "Алла",
            "F",
            "Татьяна Владимировна"
    })
    @DisplayName("Должен возвращать true для валидного имени")
    void shouldReturnTrueForValidName(String name) {
        assertTrue(UserValidationUtil.isValidName(name));
    }

    /**
     * Проверяет невалидность имен с недопустимыми символами и вывод соответствующего сообщения.
     *
     * @param name имя для проверки
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "1",
            "%$#(",
            "John1234",
            "John_Doe",
            "Татьяна  Владимировна",
            "Татьяна12 ",
            "John ",
            " John",
            " John "
    })
    @DisplayName("Должен возвращать false и выводить сообщения для имени с недопустимыми символами")
    void shouldReturnFalseAndPrintMessageForInvalidName(String name) {
        assertFalse(UserValidationUtil.isValidName(name));
        assertEquals("Неверный формат имени." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("Неверный формат имени: " + name, listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }

    /**
     * Проверяет невалидность null имени и вывод соответствующего сообщения.
     */
    @Test
    @DisplayName("Должен возвращать false и выводить сообщение для null имени")
    void shouldReturnFalseAndPrintMessageForNullName() {
        assertFalse(UserValidationUtil.isValidName(null));
        assertEquals("Пустое имя." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("Пустое имя.", listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }

    /**
     * Проверяет невалидность пустого имени и вывод соответствующего сообщения.
     */
    @Test
    @DisplayName("Должен возвращать false и выводить сообщение для пустого имени")
    void shouldReturnFalseAndPrintMessageForEmptyName() {
        assertFalse(UserValidationUtil.isValidName(""));
        assertEquals("Пустое имя." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("Пустое имя.", listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }

    /**
     * Проверяет невалидность имени, состоящего из пробелов, и вывод соответствующего сообщения.
     *
     * @param name имя для проверки
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "     "
    })
    @DisplayName("Должен возвращать false и выводить сообщение для имени из пробелов")
    void shouldReturnFalseAndPrintMessageForSpaceName(String name) {
        assertFalse(UserValidationUtil.isValidName(name));
        assertEquals("Пустое имя." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("Пустое имя.", listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }

    /**
     * Проверяет валидность корректных email-адресов.
     *
     * @param email email для проверки
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "john@example.org",
            "john.doe@example.com",
            "john-doe@example.com",
            "jhon_doe@example.com",
            "jhon+doe@example.com",
            "John&Doe@example.com",
            "john*doe@example.com",
            "johnDoe123@example.com",
            "john@test-domain.com",
            "john@test-domain.co.uk",
            "john@web.mailservice.org",
            "a@b.uk",
            "a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.q.r.s.t.u.v.w.x.y.z@abcdefghijklmnopqrstuvwxyz.abcdefg"
    })
    @DisplayName("Должен возвращать true для валидного email")
    void shouldReturnTrueForValidEmail(String email) {
        assertTrue(UserValidationUtil.isValidEmail(email));
    }

    /**
     * Проверяет невалидность email, содержащих пробелы, и вывод соответствующего сообщения.
     *
     * @param email email для проверки
     */
    @ParameterizedTest
    @ValueSource(strings = {
            " ",
            " john@example.com",
            "john@example.com ",
            "john @example.com",
            "john@example .com",
            "john@exa mple.com",
            "john@example. com"
    })
    @DisplayName("Должен возвращать false и выводить сообщение для email содержащий пробел")
    void shouldReturnFalseForEmailWithSpaces(String email) {
        assertFalse(UserValidationUtil.isValidEmail(email));
        assertEquals("Неверный формат email." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("Неверный формат email: " + email, listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }

    /**
     * Проверяет невалидность email, содержащих запрещенные символы, и вывод соответствующего сообщения.
     *
     * @param email email для проверки
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "john@doe@example.com",
            "john#doe@example.com",
            "john$doe@example.com",
            "john\\doe@example.com",
            "john/doe@example.com",
            "john^doe@example.com",
            "john%doe@example.com",
            "john(doe@example.com",
            "john)doe@example.com",
            "john!doe@example.com",
            "john=doe@example.com",
            "john`doe@example.com",
            "john~doe@example.com",
            "john;doe@example.com",
            "john:doe@example.com",
            "john'doe@example.com",
            "john\"doe@xample.com",
            "john?doe@example.com",
            "john,doe@example.com",
            "john<doe@example.com",
            "john>doe@example.com",
            "john|doe@example.com",
            "john{doe@example.com",
            "john}doe@example.com",
            "john[doe@example.com",
            "john]doe@example.com"
    })
    @DisplayName("Должен возвращать false и выводить сообщение для email содержащих запрещенный символ")
    void shouldReturnFalseForEmailWithInvalidCharacters(String email) {
        assertFalse(UserValidationUtil.isValidEmail(email));
        assertEquals("Неверный формат email." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("Неверный формат email: " + email, listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }

    /**
     * Проверяет невалидность email с некорректным доменом и вывод соответствующего сообщения.
     *
     * @param email email для проверки
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "john@.com",
            "john@example..com",
            "john@example.c",
            "john@example.veryLongTLD",
            "john@example.",
            "john@example.com.",
            "johnexample.com",
            "a@b.c"
    })
    @DisplayName("Должен возвращать false и выводить сообщение для email с неверным доменом")
    void shouldReturnFalseForEmailWithInvalidDomain(String email) {
        assertFalse(UserValidationUtil.isValidEmail(email));
        assertEquals("Неверный формат email." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("Неверный формат email: " + email, listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }

    /**
     * Проверяет невалидность некорректных email-адресов и вывод соответствующего сообщения.
     *
     * @param email email для проверки
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "john..@example.com",
            "john..doe@example.com",
            ".john@example.com",
            ".john.@example.com",
            ".john.doe@example.com",
            ".john..doe@example.com"
    })
    @DisplayName("Должен возвращать false и выводить сообщение для невалидного email")
    void shouldReturnFalseForInvalidEmail(String email) {
        assertFalse(UserValidationUtil.isValidEmail(email));
        assertEquals("Неверный формат email." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("Неверный формат email: " + email, listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }

    /**
     * Проверяет невалидность пустого email и вывод соответствующего сообщения.
     */
    @Test
    @DisplayName("Должен возвращать false и выводить сообщение для пустого email")
    void shouldReturnFalseForEmptyEmail() {
        assertFalse(UserValidationUtil.isValidEmail(""));
        assertEquals("Пустой email." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("Пустой email.", listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }

    /**
     * Проверяет невалидность null email и вывод соответствующего сообщения.
     */
    @Test
    @DisplayName("Должен возвращать false и выводить сообщение для null email")
    void shouldReturnFalseForNullEmail() {
        assertFalse(UserValidationUtil.isValidEmail(null));
        assertEquals("Пустой email." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("Пустой email.", listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }

    /**
     * Проверяет валидность корректных значений возраста.
     *
     * @param age возраст для проверки
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10, 25, 100, 149, 150})
    @DisplayName("Должен возвращать true для валидного возраста")
    void shouldReturnTrueForValidAge(int age) {
        assertTrue(UserValidationUtil.isValidAge(age));
    }

    /**
     * Проверяет невалидность некорректных значений возраста и вывод соответствующего сообщения.
     *
     * @param age возраст для проверки
     */
    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, -50, 151, 160, 200, 1000, Integer.MAX_VALUE})
    @DisplayName("Должен возвращать false и выводить сообщение для невалидного возраста")
    void shouldReturnFalseAndPrintMessageForInvalidAge(int age) {
        assertFalse(UserValidationUtil.isValidAge(age));
        assertEquals("Возраст должен быть между " +
                MIN_AGE + " и " + MAX_AGE + "." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("Возраст вне границ: " + age, listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }

    /**
     * Проверяет валидность корректных идентификаторов.
     *
     * @param id идентификатор для проверки
     */
    @ParameterizedTest
    @ValueSource(longs = {
            1L, 20L, 50L, 100L, Long.MAX_VALUE
    })
    @DisplayName("Должен возвращать true для валидного ID")
    void shouldReturnTrueForValidId(Long id) {
        assertTrue(UserValidationUtil.isValidId(id));
    }

    /**
     * Проверяет невалидность null идентификатора и вывод соответствующего сообщения.
     */
    @Test
    @DisplayName("Должен возвращать false и выводить сообщение для null ID")
    void shouldReturnFalseForNullId() {
        assertFalse(UserValidationUtil.isValidId(null));
        assertEquals("ID не может быть null." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("ID не может быть null.", listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }

    /**
     * Проверяет невалидность идентификаторов меньше 1 и вывод соответствующего сообщения.
     *
     * @param id идентификатор для проверки
     */
    @ParameterizedTest
    @ValueSource(longs = {
            0L, -1L, -10L, -100L, Long.MIN_VALUE
    })
    @DisplayName("Должен возвращать false и выводить сообщение для ID меньше 1")
    void shouldReturnFalseForIdLessThanOne(Long id) {
        assertFalse(UserValidationUtil.isValidId(id));
        assertEquals("ID меньше " + MIN_ID + "." + System.lineSeparator(), outContent.toString());
        assertEquals(1, listAppender.list.size());
        assertEquals("ID меньше: " + MIN_ID, listAppender.list.get(0).getFormattedMessage());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
    }
}