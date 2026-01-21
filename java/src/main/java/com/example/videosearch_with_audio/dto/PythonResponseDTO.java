package com.example.videosearch_with_audio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 🔥 FIX 1: Ignore extra/unknown fields
public class PythonResponseDTO {

    // 🔥 FIX 2: Use Integer/Double instead of int/double to handle missing values
    @JsonProperty("scene_index")
    private Integer sceneIndex;

    @JsonProperty("start_time")
    private Double startTime;

    @JsonProperty("end_time")
    private Double endTime;

    private String transcript;

    @JsonProperty("vector_visual")
    private List<Double> vectorVisual;

    @JsonProperty("vector_audio")
    private List<Double> vectorAudio;

    // --- Search Fields ---
    @JsonProperty("visual_vector")
    private List<Double> visualVector;

    @JsonProperty("audio_vector")
    private List<Double> audioVector;

    public PythonResponseDTO() {}

    // Getters & Setters
    public Integer getSceneIndex() { return sceneIndex; }
    public void setSceneIndex(Integer sceneIndex) { this.sceneIndex = sceneIndex; }

    public Double getStartTime() { return startTime; }
    public void setStartTime(Double startTime) { this.startTime = startTime; }

    public Double getEndTime() { return endTime; }
    public void setEndTime(Double endTime) { this.endTime = endTime; }

    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }

    public List<Double> getVectorVisual() { return vectorVisual; }
    public void setVectorVisual(List<Double> vectorVisual) { this.vectorVisual = vectorVisual; }

    public List<Double> getVectorAudio() { return vectorAudio; }
    public void setVectorAudio(List<Double> vectorAudio) { this.vectorAudio = vectorAudio; }

    public List<Double> getVisualVector() { return visualVector; }
    public void setVisualVector(List<Double> visualVector) { this.visualVector = visualVector; }

    public List<Double> getAudioVector() { return audioVector; }
    public void setAudioVector(List<Double> audioVector) { this.audioVector = audioVector; }
}