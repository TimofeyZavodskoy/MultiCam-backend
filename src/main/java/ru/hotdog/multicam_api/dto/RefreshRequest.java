package ru.hotdog.multicam_api.dto;

import lombok.Data;

@Data
// Запрос с refresh token для обновления пары токенов.
public class RefreshRequest {
    // Старый refresh token от клиента.
    private String refreshToken;
}
