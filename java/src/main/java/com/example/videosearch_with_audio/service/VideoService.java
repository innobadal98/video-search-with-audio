package com.example.videosearch_with_audio.service;

import com.example.videosearch_with_audio.dto.PythonResponseDTO;
import com.example.videosearch_with_audio.dto.SearchResultDTO;
import com.example.videosearch_with_audio.model.VideoScene;
import com.example.videosearch_with_audio.repository.VideoSceneRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VideoService {

    @Autowired
    private VideoSceneRepository sceneRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RestTemplate restTemplate;

    private final String PYTHON_URL = "http://localhost:8000";

    // --- 1. PROCESS LOCAL VIDEO (Robust & Crash-Proof) ---
    public String processLocalVideo(String localPath) {
        // A. File Check
        File file = new File(localPath);
        if (!file.exists()) {
            throw new RuntimeException("File not found at path: " + localPath);
        }
        System.out.println("📂 Processing local file: " + localPath);

        // B. Prepare Request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            // C. Call Python (Get Raw String first)
            ResponseEntity<String> response = restTemplate.exchange(
                    PYTHON_URL + "/generate-embeddings",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            String jsonResponse = response.getBody();
            if (jsonResponse == null || jsonResponse.isEmpty()) return null;

            // D. Smart JSON Parsing (List vs Object Check)
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            List<PythonResponseDTO> dtosList = new ArrayList<>();

            if (jsonResponse.trim().startsWith("[")) {
                // Array Logic
                PythonResponseDTO[] dtos = mapper.readValue(jsonResponse, PythonResponseDTO[].class);
                dtosList = Arrays.asList(dtos);
            } else {
                // Object Logic (Error handling safety)
                try {
                    PythonResponseDTO singleDto = mapper.readValue(jsonResponse, PythonResponseDTO.class);
                    dtosList.add(singleDto);
                } catch (Exception e) {
                    System.err.println("⚠️ Unknown JSON Format: " + jsonResponse);
                    return null;
                }
            }

            // E. Save to MongoDB
            String uniqueVideoId = UUID.randomUUID().toString();
            List<VideoScene> scenesToSave = new ArrayList<>();

            for (PythonResponseDTO p : dtosList) {
                // Skip invalid entries
                if (p.getSceneIndex() == null) continue;

                VideoScene scene = new VideoScene();
                scene.setVideoId(uniqueVideoId);
                scene.setFilename(file.getName());

                // Safe Setters (Null Checks)
                scene.setSceneIndex(p.getSceneIndex());
                scene.setStartTime(p.getStartTime() != null ? p.getStartTime() : 0.0);
                scene.setEndTime(p.getEndTime() != null ? p.getEndTime() : 0.0);
                scene.setTranscript(p.getTranscript() != null ? p.getTranscript() : "");
                scene.setVectorVisual(p.getVectorVisual());
                scene.setVectorAudio(p.getVectorAudio());
                scenesToSave.add(scene);
            }

            if (!scenesToSave.isEmpty()) {
                sceneRepository.saveAll(scenesToSave);
                System.out.println("✅ Saved " + scenesToSave.size() + " scenes to DB.");
                return uniqueVideoId;
            }

        } catch (Exception e) {
            System.err.println("❌ Python Error: " + e.getMessage());
            throw new RuntimeException("Processing failed: " + e.getMessage());
        }
        return null;
    }

    // --- 2. HYBRID SEARCH LOGIC (Visual + Audio) ---
    public List<SearchResultDTO> searchVideo(String queryText) {
        String url = PYTHON_URL + "/search?text=" + queryText;

        try {
            // A. Python se vectors mangwao (Use GET because Python is @app.get)
            String jsonResponse = restTemplate.getForObject(url, String.class);

            if (jsonResponse == null) return Collections.emptyList();

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            PythonResponseDTO queryVector = mapper.readValue(jsonResponse, PythonResponseDTO.class);

            // B. Visual Search (Images me dhoondo)
            List<SearchResultDTO> visualResults = runAtlasQuery("vectorVisual", queryVector.getVisualVector());

            // C. Audio Search (Transcript me dhoondo)
            List<SearchResultDTO> audioResults = runAtlasQuery("vectorAudio", queryVector.getAudioVector());

            // D. MERGE & DEDUPLICATE (Dono lists ko mix karo)
            Map<String, SearchResultDTO> uniqueResults = new HashMap<>();

            // Pehle Visual results add karo
            for (SearchResultDTO dto : visualResults) {
                String key = dto.getFilename() + "_" + dto.getStartTime();
                dto.setMatchType("Visual Match 👁️");
                uniqueResults.put(key, dto);
            }

            // Phir Audio results add karo
            for (SearchResultDTO dto : audioResults) {
                String key = dto.getFilename() + "_" + dto.getStartTime();

                if (uniqueResults.containsKey(key)) {
                    // Agar scene pehle se hai, Best Score rakho
                    SearchResultDTO existing = uniqueResults.get(key);
                    if (dto.getScore() > existing.getScore()) {
                        existing.setScore(dto.getScore());
                        existing.setMatchType("Visual + Audio Match 🔥");
                    }
                } else {
                    dto.setMatchType("Audio Match 🗣️");
                    uniqueResults.put(key, dto);
                }
            }

            // E. Return Top 5
            return uniqueResults.values().stream()
                    .sorted(Comparator.comparingDouble(SearchResultDTO::getScore).reversed())
                    .limit(5)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("❌ Search Error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // --- HELPER: Atlas Query Runner ---
    private List<SearchResultDTO> runAtlasQuery(String pathField, List<Double> vector) {
        if (vector == null || vector.isEmpty()) return new ArrayList<>();

        org.bson.Document vectorSearchStage = new org.bson.Document("$vectorSearch",
                new org.bson.Document("index", "vector_index")
                        .append("path", pathField)
                        .append("queryVector", vector)
                        .append("numCandidates", 100)
                        .append("limit", 5)
        );

        org.bson.Document projectStage = new org.bson.Document("$project",
                new org.bson.Document("filename", 1)
                        .append("startTime", 1)
                        .append("endTime", 1)
                        .append("transcript", 1)
                        .append("score", new org.bson.Document("$meta", "vectorSearchScore"))
        );

        Aggregation aggregation = Aggregation.newAggregation(
                ctx -> vectorSearchStage,
                ctx -> projectStage
        );

        AggregationResults<org.bson.Document> results = mongoTemplate.aggregate(aggregation, "scenes", org.bson.Document.class);

        return results.getMappedResults().stream().map(doc -> SearchResultDTO.builder()
                .filename(doc.getString("filename"))
                .startTime(doc.getDouble("startTime"))
                .endTime(doc.getDouble("endTime"))
                .transcript(doc.getString("transcript"))
                .score(doc.getDouble("score"))
                .build()
        ).collect(Collectors.toList());
    }
}