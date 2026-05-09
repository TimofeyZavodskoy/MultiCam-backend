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
public class UserEntity {
    @Id
    private Long id;

    @Column("username")
    private String name;

    @Column("hashed_password")
    private String password;

    @Column("email")
    private String email;

    @Column("is_guest")
    private boolean isGuest;

    @Column("created_at")
    private LocalDateTime createdAt;
}