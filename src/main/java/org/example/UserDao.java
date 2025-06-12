package org.example;

import java.util.List;

/**
 * Интерфейс DAO для работы с сущностью {@link User}.
 */
public interface UserDao {
    boolean createUser(String name, String email, int age);

    User readUser(Long id);

    List<User> readAllUsers();

    boolean updateUser(Long id, String newName, String newEmail, int newAge);

    boolean removeUser(Long id);
}
