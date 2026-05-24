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

    @Column("image_url")
    private String imageUrl;

    @Column("json_data")
    private String jsonData;

    @Column("category")
    private String category;

    @Column("user_id")
    private Long userId;

    @Column("created_at")
    private LocalDateTime createdAt;
}