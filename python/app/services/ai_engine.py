import os
import shutil
import warnings
import whisper
from sentence_transformers import SentenceTransformer
from imageio_ffmpeg import get_ffmpeg_exe

# Ignore unnecessary warnings
warnings.filterwarnings("ignore", message="FP16 is not supported on CPU")

def setup_ffmpeg():
    """Ensures FFmpeg is available and configured for Whisper."""
    try:
        ffmpeg_src = get_ffmpeg_exe()
        project_dir = os.getcwd()     # to find autometicly ffmpeg path
        ffmpeg_dest = os.path.join(project_dir, "ffmpeg.exe")

        if not os.path.exists(ffmpeg_dest):
            shutil.copy(ffmpeg_src, ffmpeg_dest)  # avoid recopy
        # Adds project directory to system PATH Ensures Whisper can find FFmpeg at runtime
        if project_dir not in os.environ["PATH"]:        
            os.environ["PATH"] += os.pathsep + project_dir
            
    except Exception as e:
        print(f"⚠️ FFmpeg setup warning: {e}")

# Run setup immediately
setup_ffmpeg()

class AIEngine:
    def __init__(self):
        print("🧠 Loading AI Models...")
        self.visual_model = SentenceTransformer('clip-ViT-B-32')
        self.whisper_model = whisper.load_model("base") 
        self.text_model = SentenceTransformer('all-MiniLM-L6-v2')
        print("✅ Models Ready!")

    def get_image_embedding(self, image_path):
        from PIL import Image
        img = Image.open(image_path)
        return self.visual_model.encode(img).tolist()

    def transcribe_audio(self, audio_path):
        try:
            result = self.whisper_model.transcribe(
                audio_path,
                language="en",
                fp16=False,
                beam_size=5,
                best_of=5,
                temperature=0.0,
                no_speech_threshold=0.8
            )
            return result['text'].strip()
        except Exception as e:
            print(f"❌ Transcription error: {e}")
            return ""

    def get_text_embedding(self, text):
        return self.text_model.encode(text).tolist()

# Global Instance
ai_engine = AIEngine()