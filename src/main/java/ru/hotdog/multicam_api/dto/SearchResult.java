package ru.hotdog.multicam_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private String marketplace;
    private String url;
    private String icon;
}
