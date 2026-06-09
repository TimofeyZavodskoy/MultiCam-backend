package ru.hotdog.multicam_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
// Данные для регистрации пользователя.
public class Signup {
    @NotEmpty
    // Имя пользователя.
    private String name;
    @NotEmpty(message = "password couldn't be empty")
    // Пароль пользователя.
    private String password;
    @Email
    @NotEmpty(message = "email couldn't be empty")
    // Почта пользователя.
    private String email;
}
