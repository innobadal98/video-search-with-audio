package com.example.videosearch_with_audio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "scenes")
public class VideoScene {
    @Id
    private String id;

    private String videoId;
    private String filename;

    // Change int -> Integer, double -> Double (Safe for nulls)
    private Integer sceneIndex;
    private Double startTime;
    private Double endTime;

    private String transcript;

    private List<Double> vectorVisual;
    private List<Double> vectorAudio;
}