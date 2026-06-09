package ru.hotdog.multicam_api.dto;

import lombok.Data;

@Data
// Запрос для входа гостем по uuid устройства.
public class GuestRequest {
    // Уникальный uuid гостя.
    private String uuid;
}
