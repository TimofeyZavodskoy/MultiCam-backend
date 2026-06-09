package ru.hotdog.multicam_api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OCRServiceTest {

    @Test
    void normalizeCategoryPrefersScienceCategoriesOverMathMentions() {
        assertEquals("physics", OCRService.normalizeCategory("physics, not math"));
        assertEquals("chemistry", OCRService.normalizeCategory("chemistry - not math"));
        assertEquals("mixed", OCRService.normalizeCategory("mixed"));
        assertEquals("math", OCRService.normalizeCategory("math"));
    }
}
