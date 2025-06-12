package org.example;

import java.util.Scanner;

/**
 * Главный класс приложения для запуска консольного интерфейса управления пользователями.
 */
public class App {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            UserDao userDAO = new UserDaoImpl();
            UserService userService = new UserService(userDAO);
            UserConsoleUI userConsoleUI = new UserConsoleUI(userService, scanner);
            userConsoleUI.start();

        } finally {
            HibernateUtil.shutdown();
        }
    }
}
