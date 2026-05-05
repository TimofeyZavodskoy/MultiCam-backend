package ru.hotdog.multicam_api.dto;

import lombok.Data;

@Data
public class DetectedObj {
    private String label;
    private Bbox bbox;
}
