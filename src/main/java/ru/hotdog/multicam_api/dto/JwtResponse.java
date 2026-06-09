package ru.hotdog.multicam_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
// Ответ с access token и данными пользователя.
public class JwtResponse {
    // Access token для запросов.
    private String token;
    // Тип токена для заголовка Authorization.
    private String type = "Bearer";
    // Почта пользователя.
    private String email;
}
