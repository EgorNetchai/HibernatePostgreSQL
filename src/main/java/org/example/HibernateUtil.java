package org.example;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Утилитный класс для управления {@link SessionFactory} в Hibernate.
 */
public class HibernateUtil {
    private static final Logger logger = LoggerFactory.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory;
    private static final String CONFIG_FILE = System.getProperty("hibernate.config.file", "Hibernate.cfg.xml");

    static {
        StandardServiceRegistry registry = null;
        try {
            registry = new StandardServiceRegistryBuilder()
                    .configure(CONFIG_FILE)
                    .build();

            sessionFactory = new MetadataSources(registry)
                    .buildMetadata()
                    .buildSessionFactory();

        } catch (HibernateException e) {
            logger.error("Не удалось инициализировать Hibernate SessionFactory: {}", e.getMessage(), e);
            destroyRegistry(registry);
            throw new ExceptionInInitializerError("Инициализация Hibernate не удалась: " + e.getMessage());

        } catch (Throwable e) {
            logger.error("Неожиданная ошибка при инициализации Hibernate: {}", e.getMessage(), e);
            destroyRegistry(registry);
            throw new RuntimeException("Критический сбой", e);
        }
    }

    /**
     * Освобождает ресурсы {@link StandardServiceRegistry}, если он не null.
     *
     * @param registry реестр для освобождения
     */
    private static void destroyRegistry(StandardServiceRegistry registry) {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
            logger.debug("Реестр уничтожен");
        }
    }

    /**
     * Возвращает SessionFactory Hibernate.
     *
     * @return объект {@link SessionFactory}
     *
     * @throws IllegalStateException если SessionFactory не инициализирована
     */
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            logger.error("SessionFactory имеет значение null, не удалось инициализировать");
            throw new IllegalStateException("SessionFactory не был инициализирован");
        }

        return sessionFactory;
    }

    /**
     * Закрывает SessionFactory Hibernate, если она открыта.
     */
    public static void shutdown() {
        if (sessionFactory != null && sessionFactory.isOpen()) {
            Logger logger = LoggerFactory.getLogger(HibernateUtil.class);
            logger.info("Shutting down SessionFactory", new Throwable("Stack trace for debugging"));
            sessionFactory.close();
            logger.info("SessionFactory закрыт");
        } else {
            logger.info("SessionFactory null или уже закрыт");
        }
    }
}