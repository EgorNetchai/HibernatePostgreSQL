package org.example;

import java.util.Scanner;

/**
 * Класс для реализации консольного интерфейса взаимодействия с пользователем.
 */
public class UserConsoleUI {
    /**
     * Перечисление для определения действий консольного интерфейса.
     */
    private enum Action {
        CREATE("1", "Создать пользователя"),
        READ("2", "Прочитать пользователя"),
        READ_ALL("3", "Прочитать всех пользователей"),
        UPDATE("4", "Обновить пользователя"),
        DELETE("5", "Удалить пользователя"),
        EXIT("6", "Выйти из приложения");

        private final String definition;
        private final String code;

        Action(String code, String definition) {
            this.code = code;
            this.definition = definition;
        }

        public String getDefinition() {
            return definition;
        }

        public String getCode() {
            return code;
        }

        public static Action fromCode(String code) {
            for (Action action : values()) {
                if (action.code.equals(code)) {
                    return action;
                }
            }
            return null;
        }
    }

    private final Scanner scanner;
    private final UserService userService;

    public UserConsoleUI(UserService userService, Scanner scanner) {
        this.scanner = scanner;
        this.userService = userService;
    }

    /**
     * Запускает консольный интерфейс для взаимодействия с пользователем.
     */
    public void start() {
        while (true) {
            showMenu();
            String input = scanner.nextLine().trim();
            Action action = Action.fromCode(input);
            if (action == null) {
                System.out.printf("Неправильный ввод, используйте цифры от 1 до %d.%n", Action.values().length);
                waitForEnter();
                continue;
            }

            try {
                switch (action) {
                    case CREATE:
                        handleCreate();
                        break;
                    case READ:
                        handleRead();
                        break;
                    case READ_ALL:
                        System.out.println(userService.readAll());
                        break;
                    case UPDATE:
                        handleUpdate();
                        break;
                    case DELETE:
                        handleDelete();
                        break;
                    case EXIT:
                        System.out.println("Приложение завершено.");
                        return;
                }
            } catch (RuntimeException e) {
                System.out.println("Произошла ошибка: " + e.getMessage());
            }
            waitForEnter();
        }
    }

    /**
     * Обрабатывает создание пользователя.
     */
    private void handleCreate() {
        System.out.print("Введите имя: ");
        String name = scanner.nextLine().trim();
        System.out.print("Введите email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Введите возраст: ");
        String age = scanner.nextLine().trim();
        System.out.println(userService.create(name, email, age));
    }

    /**
     * Обрабатывает чтение пользователя по идентификатору.
     */
    private void handleRead() {
        System.out.print("Введите идентификатор пользователя: ");
        String id = scanner.nextLine().trim();
        System.out.println(userService.read(id));
    }

    /**
     * Обрабатывает обновление пользователя.
     */
    private void handleUpdate() {
        System.out.print("Введите идентификатор пользователя: ");
        String id = scanner.nextLine().trim();
        System.out.print("Введите новое имя: ");
        String name = scanner.nextLine().trim();
        System.out.print("Введите новый email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Введите новый возраст: ");
        String age = scanner.nextLine().trim();
        System.out.println(userService.update(id, name, email, age));
    }

    /**
     * Обрабатывает удаление пользователя.
     */
    private void handleDelete() {
        System.out.print("Введите идентификатор пользователя: ");
        String id = scanner.nextLine().trim();
        System.out.println(userService.delete(id));
    }

    /**
     * Ожидает нажатия Enter для продолжения.
     */
    private void waitForEnter() {
        System.out.println("Нажмите Enter, чтобы продолжить...");
        scanner.nextLine();
    }

    /**
     * Выводит меню консольного интерфейса.
     */
    private void showMenu() {
        System.out.println("\nМеню:");
        System.out.println("=".repeat(30));
        for (Action action : Action.values()) {
            System.out.printf("%s. %s%n", action.getCode(), action.getDefinition());
        }
        System.out.print("Выберите действие: ");
    }
}