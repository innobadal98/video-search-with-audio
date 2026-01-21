package com.example.videosearch_with_audio.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResultDTO {
    private String filename;
    private double startTime;
    private double endTime;
    private String transcript;
    private double score;
    private String matchType; // New Field: "Visual Match" or "Audio Match"
}