package ru.hotdog.multicam_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
// Ответ после анализа картинки.
public class OCRResponse {
    // Категория результата.
    private String tag;
    // Основной текст результата.
    private String result;

    // Примерная масса еды.
    private Integer mass;
    // Примерные калории еды.
    private Integer calories;
    // Белки в еде.
    private Integer proteins;
    // Жиры в еде.
    private Integer fats;
    // Углеводы в еде.
    private Integer carbs;

    // Описание картинки.
    private String description;
    // Решение задачи.
    private String solution;
    // Распознанный текст.
    private String content;
    // Короткое объяснение результата.
    private String reasoning;

    // Найденные объекты на картинке.
    private List<DetectedObj> detectedObjs;

    @JsonProperty("searchResults")
    // Ссылки на поиск найденного товара.
    private List<SearchResult> marketplaceLinks;
}


