package com.example.videosearch_with_audio.controller;

import com.example.videosearch_with_audio.dto.SearchResultDTO;
import com.example.videosearch_with_audio.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "*")
public class VideoController {

    @Autowired
    private VideoService videoService;

    // ✅ ONLY ENDPOINT: Process Local File (Accepts JSON Path)
    // URL: http://localhost:8080/api/videos/process-local
    // Body: { "filePath": "C:/videos/demo.mp4" }
    @PostMapping("/process-local")
    public ResponseEntity<?> processLocalVideo(@RequestBody Map<String, String> payload) {
        try {
            String filePath = payload.get("filePath");
            if (filePath == null || filePath.isEmpty()) {
                return ResponseEntity.badRequest().body("Error: 'filePath' is required");
            }

            String videoId = videoService.processLocalVideo(filePath);
            return ResponseEntity.ok("Video Processed Successfully! ID: " + videoId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // Existing Search Endpoint
    @GetMapping("/search")
    public ResponseEntity<List<SearchResultDTO>> searchVideo(@RequestParam("query") String query) {
        List<SearchResultDTO> results = videoService.searchVideo(query);
        return ResponseEntity.ok(results);
    }
}