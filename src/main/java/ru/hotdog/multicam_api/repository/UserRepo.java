package ru.hotdog.multicam_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hotdog.multicam_api.entity.UserEntity;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findUserById(Long id);
    Optional<UserEntity> findUserByEmail(String email);
    boolean existsUserByEmail(String email);
}
