package ru.hotdog.multicam_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.hotdog.multicam_api.repository.UserRepo;
import ru.hotdog.multicam_api.service.impl.UserDetailsImpl;

@Service
@RequiredArgsConstructor
public class UserService implements ReactiveUserDetailsService {
    private final UserRepo userRepo;

    @Override
    public Mono<UserDetails> findByUsername(String email) {
        return userRepo.findByEmail(email)
                .map(UserDetailsImpl::build)
                .cast(UserDetails.class)
                .switchIfEmpty(Mono.error(() -> new RuntimeException(
                        String.format("User with email '%s' not found", email))));
    }
}