package ru.hotdog.multicam_api.dto;

import lombok.Data;

@Data
// Один объект, который модель нашла на картинке.
public class DetectedObj {
    // Название найденного объекта.
    private String label;
    // Рамка объекта на картинке.
    private Bbox bbox;
}
