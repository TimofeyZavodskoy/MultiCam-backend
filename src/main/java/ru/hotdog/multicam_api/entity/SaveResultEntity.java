package ru.hotdog.multicam_api.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table(name = "saved_result")
@Data
public class SaveResultEntity {
    @Id
    private Long id;

    @Column()
    private String imageUrl;

    @Column()
    private String jsonData;

    @Column()
    private String category;

    @Column()
    private LocalDateTime createdAt = LocalDateTime.now();
}