package ru.hotdog.multicam_api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.hotdog.multicam_api.dto.SaveRequest;
import ru.hotdog.multicam_api.entities.SaveResult;
import ru.hotdog.multicam_api.repositories.SaveResultRepo;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/save")
@Slf4j
public class SavedResult {
    private final SaveResultRepo saveResultRepo;
    private final ObjectMapper objectMapper;

    @PostMapping("/like")
    public ResponseEntity<String> saveResult(@RequestBody SaveRequest request) {
        try {
            SaveResult result = new SaveResult();
            result.setImageUrl(request.getImageUrl());
            result.setCategory(request.getCategory());

            String compactJsonString = objectMapper.writeValueAsString(request.getClientJson());
            result.setJsonData(compactJsonString);

            saveResultRepo.save(result);
            return ResponseEntity.ok("Успешно сохранено!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка: " + e.getMessage());
        }
    }
}
