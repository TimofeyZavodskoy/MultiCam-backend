package ru.hotdog.multicam_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// Ссылка на поиск товара в маркетплейсе.
public class SearchResult {

    // Название маркетплейса.
    private String marketplace;
    // Ссылка на поиск товара.
    private String url;
    // Иконка для показа на фронте.
    private String icon;
}
