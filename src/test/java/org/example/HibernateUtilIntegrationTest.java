package org.example;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Класс для интеграционного тестирования {@link HibernateUtil}.
 * Проверяет корректность получения SessionFactory и выполнения операций с базой данных.
 */
@ExtendWith(TestEnvironmentExtension.class)
class HibernateUtilIntegrationTest {
    private ListAppender<ILoggingEvent> listAppender;

    /**
     * Настройка окружения перед каждым тестом.
     * Инициализирует логгер и добавляет ListAppender для захвата логов.
     */
    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(HibernateUtil.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    /**
     * Очистка окружения после каждого теста.
     * Останавливает и отключает ListAppender от логгера.
     */
    @AfterEach
    void tearDown() {
        listAppender.stop();
        Logger logger = (Logger) LoggerFactory.getLogger(HibernateUtil.class);
        logger.detachAppender(listAppender);
    }

    /**
     * Тестирует успешное получение SessionFactory и выполнение простого запроса.
     * Проверяет, что SessionFactory не null, открыт и может создать сессию для выполнения запроса.
     */
    @Test
    @DisplayName("Успешное получение SessionFactory")
    void testGetSessionFactorySuccess() {
        assertNotNull(HibernateUtil.getSessionFactory());
        assertTrue(HibernateUtil.getSessionFactory().isOpen());

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            assertNotNull(session);

            Integer result = session.createNativeQuery("SELECT 1", Integer.class).getSingleResult();

            assertEquals(1, result);
        }
    }
}