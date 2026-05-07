package ru.hotdog.multicam_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class Signup {
    @NotEmpty
    private String username;
    @NotEmpty(message = "password couldn't be empty")
    private String password;
    @Email
    @NotEmpty(message = "email couldn't be empty")
    private String email;
}
