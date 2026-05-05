package ru.hotdog.multicam_api.dto;

import lombok.Data;

@Data
public class Bbox {
    private double x, y, width, height;
}
