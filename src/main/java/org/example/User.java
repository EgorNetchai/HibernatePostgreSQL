package org.example;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

/**
 * Сущность, представляющая пользователя в базе данных.
 */
@Entity
@Table(name = "users")
public class User {
    /**
     * Уникальный идентификатор пользователя.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    /**
     * Имя пользователя.
     */
    @Column(name = "name")
    private String name;

    /**
     * Электронная почта пользователя, уникальное поле.
     */
    @Column(name = "email", unique = true)
    private String email;

    /**
     * Возраст пользователя.
     */
    @Column(name = "age")
    private int age;

    /**
     * Дата и время создания записи о пользователе.
     */
    @Column(name = "created_at", updatable = false, nullable = false)
    @CreationTimestamp()
    private Timestamp createdAt;

    public User() {
    }

    public User(String name, String email, int age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
