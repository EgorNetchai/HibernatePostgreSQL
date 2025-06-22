package org.example;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;

/**
 * Утилитный класс для управления транзакциями Hibernate.
 */
public class TransactionUtil {
    private static final Logger logger = LoggerFactory.getLogger(TransactionUtil.class);

    /**
     * Выполняет операцию в рамках транзакции с обработкой ошибок.
     *
     * @param operation операция, выполняемая с сессией Hibernate
     * @param <T>       тип возвращаемого значения
     *
     * @return Optional, содержащий результат операции, или Optional.empty() в случае ошибки
     */
    public static <T> Optional<T> executeInTransaction(Function<Session, T> operation) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            T result = operation.apply(session);
            transaction.commit();
            logger.debug("Transaction committed, session isOpen: {}", session.isOpen());
            return Optional.ofNullable(result);

        } catch (ConstraintViolationException e) {
            rollbackIfActive(transaction);
            logger.error("Нарушение ограничений базы данных: {}", e.getMessage(), e);
            return Optional.empty();

        } catch (JDBCException e) {
            rollbackIfActive(transaction);
            logger.error("Ошибка PostgreSQL: {}", e.getMessage(), e);
            return Optional.empty();

        } catch (HibernateException e) {
            rollbackIfActive(transaction);
            logger.error("Ошибка Hibernate: {}", e.getMessage(), e);
            return Optional.empty();

        } catch (Exception e) {
            rollbackIfActive(transaction);
            logger.error("Неожиданная ошибка во время работы: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Выполняет откат транзакции, если она активна.
     *
     * @param transaction транзакция для отката
     */
    private static void rollbackIfActive(Transaction transaction) {
        if (transaction != null && transaction.isActive()) {
            try {
                transaction.rollback();
                logger.debug("Транзакция откатилась");
            } catch (Exception e) {
                logger.error("Ошибка при откате транзакции: {}", e.getMessage(), e);
            }
        }
    }
}
