package ru.hotdog.multicam_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hotdog.multicam_api.entity.SaveResultEntity;

@Repository
public interface SaveResultRepo extends JpaRepository<SaveResultEntity, Long> {
}
