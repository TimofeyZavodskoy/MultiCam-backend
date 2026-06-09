package ru.hotdog.multicam_api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
// Таблица с пользователями приложения.
public class UserEntity {
    @Id
    // Номер пользователя в базе.
    private Long id;

    @Column("username")
    // Имя пользователя.
    private String name;

    @Column("hashed_password")
    // Хеш пароля пользователя.
    private String password;

    @Column("email")
    // Почта пользователя.
    private String email;

    @Column("is_guest")
    // Показывает, что пользователь гостевой.
    private boolean isGuest;

    @Column("created_at")
    // Время создания пользователя.
    private LocalDateTime createdAt;
}
