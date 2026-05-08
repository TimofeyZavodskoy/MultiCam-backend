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

    @Column()
    private String name;

    @Column()
    private String password;

    @Column()
    private String email;

    @Column()
    private LocalDateTime createdAt = LocalDateTime.now();
}