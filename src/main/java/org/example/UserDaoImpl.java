package org.example;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Реализация интерфейса {@link UserDao} для работы с сущностью {@link User} с использованием Hibernate.
 */
public class UserDaoImpl implements UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);

    /**
     * Создает нового пользователя в базе данных.
     *
     * @param name  имя пользователя
     * @param email адрес электронной почты пользователя
     * @param age   возраст пользователя
     * @return true, если пользователь успешно создан, иначе false
     * @throws IllegalArgumentException если email уже существует
     */
    @Override
    public boolean createUser(String name, String email, int age) {
        Boolean result = TransactionUtil.executeInTransaction(session -> {
            UserValidationUtil.checkEmailExists(session, email);
            User user = new User(name, email, age);
            session.persist(user);
            logger.info("Пользователь создан: id={}, name={}, email{}, age={}", user.getId(), name, email, age);
            return true;
        });

        return result != null ? result : false;
    }

    /**
     * Получает пользователя из базы данных по его идентификатору.
     *
     * @param id идентификатор пользователя
     * @return объект {@link User}, если пользователь найден, иначе null
     */
    @Override
    public User readUser(Long id) {
        return TransactionUtil.executeInTransaction(session -> {
            User foundUser = findUser(session, id);
            if (foundUser != null) {
                logger.info("Пользователь с идентификатором {} прочитан", id);
            }
            return foundUser;
        });
    }

    /**
     * Получает список всех пользователей из базы данных.
     *
     * @return список объектов {@link User}, или пустой список, если пользователи не найдены
     */
    @Override
    public List<User> readAllUsers() {
        List<User> users = TransactionUtil.executeInTransaction(session -> {
            List<User> result = session.createSelectionQuery("FROM User", User.class).getResultList();
            logger.info("Список пользователей прочитан");
            return result;
        });
        return users != null ? users : List.of();
    }

    /**
     * Обновляет данные пользователя в базе данных.
     *
     * @param id       идентификатор пользователя
     * @param newName  новое имя пользователя
     * @param newEmail новый адрес электронной почты
     * @param newAge   новый возраст пользователя
     * @return true, если пользователь успешно обновлен, иначе false
     * @throws IllegalArgumentException если новый email уже существует
     */
    @Override
    public boolean updateUser(Long id, String newName, String newEmail, int newAge) {
        Boolean result = TransactionUtil.executeInTransaction(session -> {
            User user = findUser(session, id);
            if (user != null) {
                if (!user.getEmail().equals(newEmail)) {
                    UserValidationUtil.checkEmailExists(session, newEmail);
                }
                user.setName(newName);
                user.setEmail(newEmail);
                user.setAge(newAge);
                logger.info("Пользователь с идентификатором {} " +
                        "обновлен: имя={}, email={}, возраст={}", id, newName, newEmail, newAge);
                return true;
            }
            return false;
        });

        return result != null ? result : false;
    }

    /**
     * Удаляет пользователя из базы данных по его идентификатору.
     *
     * @param id идентификатор пользователя
     * @return true, если пользователь успешно удален, иначе false
     * @throws IllegalArgumentException если пользователь с указанным идентификатором не существует
     */
    @Override
    public boolean removeUser(Long id) {
        Boolean result = TransactionUtil.executeInTransaction(session -> {
            User user = findUser(session, id);
            if (user != null) {
                session.remove(user);
                logger.info("Пользователь с идентификатором {} удален", id);
                return true;
            } else {
                throw new IllegalArgumentException("Пользователь с таким идентификатором: " + id + " не существует");
            }
        });

        return result != null ? result : false;
    }

    /**
     * Ищет пользователя по идентификатору и логирует его отсутствие.
     *
     * @param session сессия Hibernate
     * @param id      идентификатор пользователя
     * @return объект {@link User} или null, если пользователь не найден
     */
    private User findUser(Session session, Long id) {
        User user = session.find(User.class, id);

        if (user == null) {
            logger.info("Невозможно найти пользователя с идентификатором {}", id);
            return null;
        }

        return user;
    }
}
