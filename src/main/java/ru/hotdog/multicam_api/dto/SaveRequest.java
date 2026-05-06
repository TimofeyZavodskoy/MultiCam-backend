package ru.hotdog.multicam_api.dto;

import lombok.Data;

@Data
public class SaveRequest {
    private String imageUrl;
    private OCRResponse clientJson;
    private String category;
}
