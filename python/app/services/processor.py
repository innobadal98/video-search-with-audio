import os
import cv2
import uuid
import subprocess
from scenedetect import VideoManager, SceneManager
from scenedetect.detectors import ContentDetector
from moviepy import VideoFileClip
from app.services.ai_engine import ai_engine
from imageio_ffmpeg import get_ffmpeg_exe

# --- 1. VIDEO PROCESSING LOGIC ---
def generate_vectors(video_path):
    results = [] # list
    filename = os.path.basename(video_path)
    print(f"🎬 Processing video: {filename}")

    # A. Video Check
    # vailidation if video is currepted ffmpeg trow the error
    try:
        full_video = VideoFileClip(video_path) 
        video_duration = full_video.duration
        full_video.close()
    except Exception as e:
        print(f"❌ Video Load Error: {e}")
        # Error aane par bhi List return karo
        return [{
            "scene_index": 0,
            "transcript": "Corrupt Video File",
            "start_time": 0.0,
            "end_time": 0.0,
            "vector_visual": [],
            "vector_audio": []
        }]

    # B. Scene Detection
    final_scenes = []
    try:
        video_manager = VideoManager([video_path])
        scene_manager = SceneManager()
        # threshold - sence change senstivity  min_scene_len - avoid micro cuts
        scene_manager.add_detector(ContentDetector(threshold=40.0, min_scene_len=45))  
        video_manager.start()
        scene_manager.detect_scenes(frame_source=video_manager)
        detected_scenes = scene_manager.get_scene_list(video_manager.get_base_timecode())
        
        # return -  [start_sec end_sec]
        if detected_scenes:
            print(f"   ✅ Detected {len(detected_scenes)} scenes.")
            for scene in detected_scenes:
                final_scenes.append((scene[0].get_seconds(), scene[1].get_seconds()))
        else:
            final_scenes.append((0.0, video_duration))
    except Exception as e:
        print(f"   ⚠️ Scene detection warning: {e}")
        final_scenes.append((0.0, video_duration))

    # C. FFmpeg Setup
    # return real path C:\Users\...\imageio_ffmpeg\binaries\ffmpeg-win64-v4.2.2.exe
    ffmpeg_path = get_ffmpeg_exe()

    # D. Processing Loop
    for i, (start_sec, end_sec) in enumerate(final_scenes):
        duration = end_sec - start_sec
        if duration < 1.0: continue   # less then 1 seconds secne are avoided

        unique_id = str(uuid.uuid4())[:8]
        temp_img = f"temp/{unique_id}.jpg"
        temp_audio = f"temp/{unique_id}.wav"

        segment_info = {
            "scene_index": i,
            "start_time": start_sec,
            "end_time": end_sec,
            "transcript": "",
            "vector_visual": [],
            "vector_audio": []
        }

        try:
            # 1. Visual Frame
            cap = cv2.VideoCapture(video_path)
            cap.set(cv2.CAP_PROP_POS_MSEC, (start_sec + duration/2) * 1000) # get clip of middle frame
            success, frame = cap.read()
            cap.release()
            
            if success:
                cv2.imwrite(temp_img, frame)
                segment_info["vector_visual"] = ai_engine.get_image_embedding(temp_img)

            # 2. Audio Extraction
            padding = 0.5 
            audio_start = max(0, start_sec - padding)
            audio_duration = duration + (2 * padding)
            # ffmpeg cmand y - override exiting file, ss - start time, t - duration 
            # pcm_s16le raw WAV file
            command = [
                ffmpeg_path, "-y", "-i", video_path, "-ss", str(audio_start), "-t", str(audio_duration),
                "-vn", "-ac", "1", "-ar", "16000", "-c:a", "pcm_s16le", "-af", "volume=2.0", temp_audio
            ]
            # silent exicution if error come dint breck the pipline
            subprocess.run(command, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            # 1000 byte
            if os.path.exists(temp_audio) and os.path.getsize(temp_audio) > 1000:
                transcript = ai_engine.transcribe_audio(temp_audio)
                if transcript and transcript.strip():
                    segment_info["transcript"] = transcript
                    segment_info["vector_audio"] = ai_engine.get_text_embedding(transcript)

            # Success: Add to list
            results.append(segment_info)

        except Exception as e:
            print(f"⚠️ Error in segment {i}: {e}")
            continue 

        finally:
            if os.path.exists(temp_img): os.remove(temp_img)
            if os.path.exists(temp_audio): os.remove(temp_audio)

    # Empty Check
    if not results:
        return []

    return results

# --- 2. SEARCH LOGIC ---
def process_search_query(query_text: str):
    if not query_text: return None
    visual_search_vector = ai_engine.visual_model.encode(query_text).tolist()
    audio_search_vector = ai_engine.text_model.encode(query_text).tolist()
    
    # Java expects object for search
    return {
        "visual_vector": visual_search_vector, 
        "audio_vector": audio_search_vector
    }