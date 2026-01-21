import os
import shutil
from fastapi import FastAPI, UploadFile, File
from typing import List, Dict
from app.services.processor import generate_vectors, process_search_query
# shutil - saves upload file safely
app = FastAPI()

@app.on_event("startup")
def startup_event():
    os.makedirs("temp", exist_ok=True)
    os.makedirs("uploads", exist_ok=True)

# --- Endpoint 1: Video Ingestion (POST hi rahega) ---
@app.post("/generate-embeddings", response_model=List[dict])
async def create_embeddings(file: UploadFile = File(...)):
    file_path = f"uploads/{file.filename}"
    
    try:
        # 1. File Save
        # we send only pointer form postman so saving video is midentry it save file on uploads folder
        with open(file_path, "wb+") as buffer:
            shutil.copyfileobj(file.file, buffer)
            
        # 2. Process
        vectors_data = generate_vectors(file_path)
        
        if not vectors_data:
            return []
            
        return vectors_data 

    except Exception as e:
        print(f"❌ Error: {e}")
        return [{
            "scene_index": -1,
            "transcript": f"Error: {str(e)}",
            "start_time": 0.0,
            "end_time": 0.0,
            "vector_visual": [],
            "vector_audio": []
        }]
    
    finally:
        pass

# --- Endpoint 2: Search Logic (CHANGED TO GET) ---
# Ab Java ka GET request yahan land karega ✅
@app.get("/search")
async def search_endpoint(text: str):
    try:
        print(f"🔍 Searching for: {text}") # Debug log
        vectors = process_search_query(text)
        
        if not vectors:
            return {"visual_vector": [], "audio_vector": []}

        return vectors

    except Exception as e:
        print(f"❌ Search Error: {e}")
        return {"visual_vector": [], "audio_vector": []}