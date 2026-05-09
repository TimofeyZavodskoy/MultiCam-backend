package ru.hotdog.multicam_api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
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

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<OCRResponse>> process(@RequestPart("image") FilePart file) {
        return file.content()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .collectList()
                .map(list -> {
                    int total = list.stream().mapToInt(b -> b.length).sum();
                    byte[] all = new byte[total];
                    int offset = 0;
                    for (byte[] b : list) { System.arraycopy(b, 0, all, offset, b.length); offset += b.length; }
                    return all;
                })
                .flatMap(ocrService::processRequest)
                .map(ResponseEntity::ok);
    }
}