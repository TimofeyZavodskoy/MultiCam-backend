package ru.hotdog.multicam_api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.hotdog.multicam_api.dto.OCRResponse;
import ru.hotdog.multicam_api.service.OCRService;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Slf4j
public class OCRController {

    private final OCRService ocrService;

    // Измени тип принимаемого файла с MultipartFile на FilePart
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<OCRResponse>> process(@RequestPart("image") org.springframework.http.codec.multipart.FilePart file) {
        log.info("Получен запрос на распознавание: {}, размер не определен напрямую", file.filename());

        // Чтение байтов из FilePart (он реактивный)
        return file.content()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .collectList()
                .map(list -> {
                    int totalSize = list.stream().mapToInt(b -> b.length).sum();
                    byte[] allBytes = new byte[totalSize];
                    int offset = 0;
                    for (byte[] b : list) {
                        System.arraycopy(b, 0, allBytes, offset, b.length);
                        offset += b.length;
                    }
                    return allBytes;
                })
                .toFuture()
                .thenCompose(imageBytes -> ocrService.processRequest(imageBytes))
                .thenApply(ResponseEntity::ok);
    }
}