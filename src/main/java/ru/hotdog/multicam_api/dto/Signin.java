package ru.hotdog.multicam_api.dto;

import lombok.Data;

@Data
public class Signin {
    private String email;
    private String password;
}