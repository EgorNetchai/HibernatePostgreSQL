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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Интеграционные тесты для класса {@link TransactionUtil}, использующие Testcontainers и PostgreSQL.
 */
@ExtendWith(TestEnvironmentExtension.class)
@Testcontainers
class TransactionUtilIntegrationTest {

    private ListAppender<ILoggingEvent> listAppender;

    /**
     * Очищает базу данных и настраивает логгер перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();

            Long tableExists = session.createNativeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'test'", Long.class
            ).getSingleResult();

            if (tableExists > 0) {
                session.createNativeQuery("TRUNCATE TABLE test RESTART IDENTITY", Void.class).executeUpdate();
            }
            session.getTransaction().commit();

        } catch (Exception e) {
            throw new RuntimeException("Failed to clear database", e);
        }

        Logger logger = (Logger) LoggerFactory.getLogger(TransactionUtil.class);
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
        Logger logger = (Logger) LoggerFactory.getLogger(TransactionUtil.class);
        logger.detachAppender(listAppender);
    }

    /**
     * Тестирует успешное выполнение операции в транзакции.
     */
    @Test
    @DisplayName("Успешное выполнение транзакции")
    void testExecuteInTransactionSuccess() {
        Optional<String> result = TransactionUtil.executeInTransaction(session -> {
            session.createNativeQuery("CREATE TABLE test (id SERIAL PRIMARY KEY)", Void.class).executeUpdate();
            return "Table created";
        });

        assertTrue(result.isPresent());
        assertEquals("Table created", result.get());

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createNativeQuery("SELECT COUNT(*) FROM test", Long.class).getSingleResult();
            assertEquals(0, count);
        }
    }

    /**
     * Тестирует обработку нарушения ограничений базы данных в транзакции.
     */
    @Test
    @DisplayName("Обработка нарушения ограничений базы данных")
    void testExecuteInTransactionConstraintViolation() {
        Optional<Void> result = TransactionUtil.executeInTransaction(session -> {
            session.createNativeQuery("CREATE TABLE test (id SERIAL PRIMARY KEY, name VARCHAR(50) UNIQUE)", Void.class)
                    .executeUpdate();
            session.createNativeQuery("INSERT INTO test (name) VALUES ('test')", Void.class).executeUpdate();
            session.createNativeQuery("INSERT INTO test (name) VALUES ('test')", Void.class).executeUpdate();
            return null;
        });

        assertFalse(result.isPresent());

        List<ILoggingEvent> logs = listAppender.list;

        assertTrue(logs.stream().anyMatch(event ->
                event.getFormattedMessage().contains("Нарушение ограничений базы данных") &&
                        event.getLevel() == Level.ERROR));
    }

    /**
     * Тестирует обработку исключения Hibernate при обращении к несуществующей таблице.
     */
    @Test
    @DisplayName("Обработка исключения Hibernate")
    void testExecuteInTransactionHibernateException() {
        Optional<Void> result = TransactionUtil.executeInTransaction(session -> {
            session.createNativeQuery("SELECT * FROM nonexistent_table", Object.class).executeUpdate();
            return null;
        });

        assertFalse(result.isPresent());

        List<ILoggingEvent> logs = listAppender.list;

        assertTrue(logs.stream().anyMatch(event ->
                event.getFormattedMessage().contains("Ошибка PostgreSQL") &&
                        event.getLevel() == Level.ERROR));
    }
}