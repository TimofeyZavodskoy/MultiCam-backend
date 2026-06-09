package ru.hotdog.multicam_api.dto;

import lombok.Data;

@Data
// Данные для входа пользователя.
public class Signin {
    // Почта пользователя.
    private String email;
    // Пароль пользователя.
    private String password;
}
