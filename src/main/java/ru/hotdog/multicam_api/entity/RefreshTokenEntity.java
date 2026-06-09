package ru.hotdog.multicam_api.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(name = "refresh_tokens")
@Data
// Таблица с refresh token для пользователей.
public class RefreshTokenEntity {
    @Id
    // Номер записи в базе.
    private Long id;

    @Column("user_id")
    // Номер пользователя, которому принадлежит токен.
    private Long userId;

    @Column("token")
    // Сам refresh token.
    private String token;

    @Column("expires_at")
    // Время, когда токен перестает работать.
    private LocalDateTime expiresAt;

    @Column("created_at")
    // Время создания токена.
    private LocalDateTime createdAt;
}
