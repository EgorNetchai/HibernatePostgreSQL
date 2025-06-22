package org.example;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Сервисный класс для обработки операций с пользователями, включая валидацию данных
 * и взаимодействие с DAO.
 */
public class UserService {
    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDao userDAO;

    /**
     * Конструктор сервиса для работы с пользователями.
     *
     * @param userDAO DAO для работы с пользователями
     */
    public UserService(UserDao userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Создает нового пользователя с указанными данными.
     *
     * @param name  имя пользователя
     * @param email адрес электронной почты
     * @param age   возраст пользователя
     * @return сообщение о результате операции
     */
    public String create(String name, String email, String age) {
        try {
            int parsedAge = Integer.parseInt(age.trim());
            if (UserValidationUtil.isValidName(name) &&
                    UserValidationUtil.isValidEmail(email) &&
                    UserValidationUtil.isValidAge(parsedAge)) {
                if (userDAO.createUser(name, email, parsedAge)) {
                    return "Пользователь успешно создан.";
                } else {
                    return "Не удалось создать пользователя.";
                }
            }
            return "Некорректные данные.";

        } catch (NumberFormatException e) {
            return "Введите допустимое целое число для возраста.";

        } catch (IllegalArgumentException e) {
            return "Не удалось создать пользователя: " + e.getMessage();

        } catch (HibernateException e) {
            logger.error("Ошибка базы данных при создании пользователя: {}", e.getMessage(), e);
            return "Произошла ошибка базы данных. Попробуйте еще раз позже.";
        }
    }

    /**
     * Получает информацию о пользователе по его идентификатору.
     *
     * @param id идентификатор пользователя
     * @return сообщение с данными пользователя или об ошибке
     */
    public String read(String id) {
        try {
            long parsedId = Long.parseLong(id.trim());
            if (UserValidationUtil.isValidId(parsedId)) {
                User user = userDAO.readUser(parsedId);
                return userDataOutput(user);
            }
            return "Некорректный идентификатор.";

        } catch (NumberFormatException e) {
            return "Введите допустимое целое число для идентификатора.";
        } catch (HibernateException e) {
            logger.error("Ошибка базы данных при чтении пользователя: {}", e.getMessage(), e);
            return "Произошла ошибка базы данных. Попробуйте еще раз позже.";
        }
    }

    /**
     * Формирует строковое представление данных пользователя.
     *
     * @param user пользователь для вывода
     * @return строковое представление данных пользователя
     */
    private String userDataOutput(User user) {
        if (user == null) {
            return "Пользователь не найден.";
        }

        return String.format("Пользователь с идентификатором %d:\nСоздан в: %s\nИмя: %s, email: %s, возраст: %d",
                user.getId(), user.getCreatedAt(), user.getName(), user.getEmail(), user.getAge());
    }

    /**
     * Получает список всех пользователей.
     *
     * @return строковое представление списка пользователей или сообщение об ошибке
     */
    public String readAll() {
        List<User> users = userDAO.readAllUsers();
        if (users == null) {
            logger.error("Не удалось прочитать пользователей из базы данных");
            return "Ошибка чтения пользователей.";
        }
        if (users.isEmpty()) {
            return "Пользователи не найдены.";
        }

        StringBuilder sb = new StringBuilder("Пользователи:\n");
        sb.append(String.format("%-5s %-20s %-30s %-5s %-20s%n", "ID", "Имя", "Email", "Возраст", "Создан"));

        for (User user : users) {
            sb.append(String.format("%-5d %-20s %-30s %-5d %-20s%n",
                    user.getId(), user.getName(), user.getEmail(), user.getAge(), user.getCreatedAt()));
        }
        return sb.toString();
    }

    /**
     * Обновляет данные пользователя по его идентификатору.
     *
     * @param id    идентификатор пользователя
     * @param name  новое имя
     * @param email новый email
     * @param age   новый возраст
     * @return сообщение о результате операции
     */
    public String update(String id, String name, String email, String age) {
        try {
            long parsedId = Long.parseLong(id.trim());
            int parsedAge = Integer.parseInt(age.trim());
            if (UserValidationUtil.isValidId(parsedId) &&
                    UserValidationUtil.isValidName(name) &&
                    UserValidationUtil.isValidEmail(email) &&
                    UserValidationUtil.isValidAge(parsedAge)) {
                if (userDAO.updateUser(parsedId, name, email, parsedAge)) {
                    return "Пользователь обновлен успешно.";
                } else {
                    return "Не удалось обновить пользователя. Пользователь не найден или адрес электронной почты уже существует.";
                }
            }
            return "Некорректные данные.";

        } catch (NumberFormatException e) {
            return "Введите допустимое целое число для идентификатора или возраста.";

        } catch (IllegalArgumentException e) {
            return "Не удалось обновить пользователя: " + e.getMessage();

        } catch (HibernateException e) {
            logger.error("Произошла ошибка базы данных при обновлении пользователя: {}", e.getMessage(), e);
            return "Произошла ошибка базы данных. Попробуйте еще раз позже.";
        }
    }

    /**
     * Удаляет пользователя по его идентификатору.
     *
     * @param id идентификатор пользователя
     * @return сообщение о результате операции
     */
    public String delete(String id) {
        try {
            long parsedId = Long.parseLong(id.trim());
            if (UserValidationUtil.isValidId(parsedId)) {
                if (userDAO.removeUser(parsedId)) {
                    return "Пользователь успешно удален.";
                } else {
                    return "Не удалось удалить пользователя. Пользователь не найден.";
                }
            }
            return "Некорректный идентификатор.";

        } catch (NumberFormatException e) {
            return "Введите допустимое целое число для идентификатора.";
        } catch (HibernateException e) {
            logger.error("Произошла ошибка базы данных при удалении пользователя: {}", e.getMessage(), e);
            return "Произошла ошибка базы данных. Попробуйте еще раз позже.";
        }
    }
}