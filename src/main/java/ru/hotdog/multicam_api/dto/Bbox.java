package ru.hotdog.multicam_api.dto;

import lombok.Data;

@Data
// Прямоугольник найденного объекта на картинке.
public class Bbox {
    // Координаты и размер рамки объекта.
    private double x, y, width, height;
}
