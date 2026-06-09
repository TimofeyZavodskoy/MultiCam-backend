package ru.hotdog.multicam_api.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table(name = "saved_result")
@Data
// Таблица с сохраненными результатами анализа.
public class SaveResultEntity {
    @Id
    // Номер сохраненного результата.
    private Long id;

    @Column("image_url")
    // Ссылка на картинку.
    private String imageUrl;

    @Column("json_data")
    // Результат анализа в виде JSON.
    private String jsonData;

    @Column("category")
    // Категория результата.
    private String category;

    @Column("user_id")
    // Номер пользователя, который сохранил результат.
    private Long userId;

    @Column("created_at")
    // Время сохранения результата.
    private LocalDateTime createdAt;
}
