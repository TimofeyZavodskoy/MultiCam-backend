package ru.hotdog.multicam_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OCRResponse {
    private String tag;
    private String result;

    private Integer mass;
    private Integer calories;
    private Integer proteins;
    private Integer fats;
    private Integer carbs;

    private String description;
    private String solution;
    private String content;
    private String reasoning;

    private List<DetectedObj> detectedObjs;

    @JsonProperty("searchResults")
    private List<SearchResult> marketplaceLinks;
}


