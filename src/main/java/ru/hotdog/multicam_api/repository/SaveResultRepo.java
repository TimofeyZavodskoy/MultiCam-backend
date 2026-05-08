package ru.hotdog.multicam_api.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import ru.hotdog.multicam_api.entity.SaveResultEntity;

@Repository
public interface SaveResultRepo extends R2dbcRepository<SaveResultEntity, Long> {
}