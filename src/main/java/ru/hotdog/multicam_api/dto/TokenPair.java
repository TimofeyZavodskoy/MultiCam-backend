package ru.hotdog.multicam_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
// Пара токенов для входа и обновления сессии.
public class TokenPair {
    // Access token для защищенных запросов.
    private String accessToken;
    // Refresh token для обновления access token.
    private String refreshToken;
}
