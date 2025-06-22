package org.example;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Класс для тестирования метода завершения работы {@link HibernateUtil}.
 * Проверяет корректное закрытие SessionFactory и логирование соответствующих сообщений.
 */
@ExtendWith(TestEnvironmentExtension.class)
class HibernateUtilShutdownTest {
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
     * Тестирует успешное завершение работы SessionFactory.
     * Проверяет, что SessionFactory закрывается корректно и логирует соответствующее сообщение.
     */
    @Test
    @DisplayName("Успешное завершение работы SessionFactory")
    void testShutdown() {
        assertNotNull(HibernateUtil.getSessionFactory());

        HibernateUtil.shutdown();

        assertTrue(HibernateUtil.getSessionFactory().isClosed());

        List<ILoggingEvent> logs = listAppender.list;

        assertTrue(logs.stream().anyMatch(event ->
                event.getFormattedMessage().contains("SessionFactory закрыт") &&
                        event.getLevel() == Level.INFO));
    }
}