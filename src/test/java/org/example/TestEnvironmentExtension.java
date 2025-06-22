package org.example;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Расширение JUnit для настройки и очистки тестового окружения.
 * Управляет запуском и остановкой контейнера PostgreSQL, а также инициализацией и закрытием Hibernate SessionFactory.
 */
public class TestEnvironmentExtension implements BeforeAllCallback, AfterAllCallback {
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password");

    private static volatile boolean initialized = false;

    /**
     * Настройка тестового окружения перед выполнением всех тестов.
     * Запускает контейнер PostgreSQL, устанавливает системные свойства и инициализирует SessionFactory.
     *
     * @param context контекст расширения JUnit
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        synchronized (TestEnvironmentExtension.class) {
            if (!initialized) {
                if (!postgres.isRunning()) {
                    postgres.start();
                }

                System.setProperty("hibernate.config.file", "hibernate-test.cfg.xml");
                System.setProperty("test.hibernate.connection.url", postgres.getJdbcUrl());
                System.setProperty("test.hibernate.connection.username", postgres.getUsername());
                System.setProperty("test.hibernate.connection.password", postgres.getPassword());

                try {
                    HibernateUtil.getSessionFactory(); // Инициализация SessionFactory
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize SessionFactory", e);
                }

                initialized = true;
            }
        }
    }


    /**
     * Очистка тестового окружения после выполнения всех тестов.
     * Закрывает SessionFactory, останавливает контейнер PostgreSQL и сбрасывает флаг инициализации.
     *
     * @param context контекст расширения JUnit
     */
    @Override
    public void afterAll(ExtensionContext context) {
        ExtensionContext.Store globalStore = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        if (globalStore.get("testEnvironment") == null) {
            synchronized (TestEnvironmentExtension.class) {
                if (initialized) {
                    HibernateUtil.shutdown();
                    postgres.stop();
                    initialized = false;
                    globalStore.put("testEnvironment", "stopped");
                }
            }
        }
    }
}