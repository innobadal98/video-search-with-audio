package com.example.videosearch_with_audio.repository;



import com.example.videosearch_with_audio.model.VideoScene;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoSceneRepository extends MongoRepository<VideoScene, String> {
}