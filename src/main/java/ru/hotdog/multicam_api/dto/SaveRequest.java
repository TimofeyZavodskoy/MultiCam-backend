package ru.hotdog.multicam_api.dto;

import lombok.Data;

@Data
// Запрос на сохранение результата анализа.
public class SaveRequest {
    // Ссылка на картинку.
    private String imageUrl;
    // Результат анализа с клиента.
    private OCRResponse clientJson;
    // Категория сохраненного результата.
    private String category;
}
