package ru.hotdog.multicam_api.service.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.hotdog.multicam_api.entity.UserEntity;
import java.util.Collection;
import java.util.List;

@Data
@AllArgsConstructor
// Данные пользователя для Spring Security.
public class UserDetailsImpl implements UserDetails {
    // Имя пользователя.
    private String name;
    // Хеш пароля пользователя.
    private String password;
    // Почта пользователя.
    private String email;
    // Номер пользователя.
    private Long id;

    @Override
    // Возвращает роли пользователя.
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    // Возвращает почту как логин.
    public String getUsername() {
        return email;
    }

    @Override
    // Проверяет, что аккаунт не истек.
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    // Проверяет, что аккаунт не заблокирован.
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    // Проверяет, что пароль не истек.
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    // Проверяет, что аккаунт включен.
    public boolean isEnabled() {
        return true;
    }

    // Создает объект безопасности из пользователя.
    public static UserDetailsImpl build(UserEntity user) {
        return new UserDetailsImpl(
                user.getName(),
                user.getPassword(),
                user.getEmail(),
                user.getId()
        );
    }
}
