package ru.hotdog.multicam_api.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(name = "refresh_tokens")
@Data
public class RefreshTokenEntity {
    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("token")
    private String token;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Column("created_at")
    private LocalDateTime createdAt;
}
