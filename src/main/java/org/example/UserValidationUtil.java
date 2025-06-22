package org.example;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилитный класс для валидации данных пользователя.
 */
public class UserValidationUtil {
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final Pattern pattern = Pattern.compile(EMAIL_REGEX);
    private static final int MAX_AGE = 150;
    private static final int MIN_AGE = 0;
    private static final long MIN_ID = 1;

    private static final Logger logger = LoggerFactory.getLogger(UserValidationUtil.class);

    /**
     * Проверяет существование email в базе данных.
     *
     * @param session сессия Hibernate
     * @param email   email для проверки
     *
     * @throws IllegalArgumentException если email уже существует
     */
    public static void checkEmailExists(Session session, String email) {
        boolean exists = session.createSelectionQuery("FROM User WHERE email = :email", User.class)
                .setParameter("email", email)
                .uniqueResult() != null;

        if (exists) {
            logger.error("Email {} уже существует", email);
            throw new IllegalArgumentException("Email " + email + " уже существует");
        }
    }

    /**
     * Проверяет корректность имени пользователя.
     *
     * @param name имя для проверки
     *
     * @return true, если имя корректно, иначе false
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            logger.debug("Пустое имя.");
            System.out.println("Пустое имя.");
            return false;
        }

        if (!name.matches("[a-zA-Z\\p{L}]+( [a-zA-Z\\p{L}]+)*")) {
            logger.debug("Неверный формат имени: {}", name);
            System.out.println("Неверный формат имени.");
            return false;
        }

        return true;
    }

    /**
     * Проверяет корректность email.
     *
     * @param email email для проверки
     *
     * @return true, если email корректен, иначе false
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            logger.debug("Пустой email.");
            System.out.println("Пустой email.");
            return false;
        }

        Matcher matcher = pattern.matcher(email);

        if (!matcher.matches()) {
            logger.debug("Неверный формат email: {}", email);
            System.out.println("Неверный формат email.");
            return false;
        }

        return true;
    }

    /**
     * Проверяет корректность возраста.
     *
     * @param age возраст для проверки
     *
     * @return true, если возраст находится в допустимом диапазоне, иначе false
     */
    public static boolean isValidAge(int age) {
        if (age < MIN_AGE || age > MAX_AGE) {
            logger.debug("Возраст вне границ: {}", age);
            System.out.println("Возраст должен быть между " + MIN_AGE + " и " + MAX_AGE + ".");
            return false;
        }

        return true;
    }

    /**
     * Проверяет корректность идентификатора.
     *
     * @param id идентификатор для проверки
     *
     * @return true, если идентификатор корректен, иначе false
     */
    public static boolean isValidId(Long id) {
        if(id == null) {
            logger.debug("ID не может быть null.");
            System.out.println("ID не может быть null.");
            return false;
        }

        if (id < MIN_ID) {
            logger.debug("ID меньше: {}", MIN_ID);
            System.out.println("ID меньше " + MIN_ID + ".");
            return false;
        }

        return true;
    }
}