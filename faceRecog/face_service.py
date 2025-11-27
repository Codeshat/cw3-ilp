from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
import numpy as np
from PIL import Image
from insightface.app import FaceAnalysis
import cv2

app = FastAPI()

# Initialize face recognition model
app_face = FaceAnalysis(name="antelopev2", providers=['CPUExecutionProvider'])
app_face.prepare(ctx_id=0)

def get_embedding(image_bytes):
    image = Image.open(image_bytes).convert("RGB")
    img_np = np.array(image)

    faces = app_face.get(img_np)

    if len(faces) == 0:
        return None

    # Take the first detected face
    return faces[0].embedding

@app.post("/match")
async def match_faces(reference: UploadFile = File(...),
                      candidate: UploadFile = File(...)):

    ref_emb = get_embedding(reference.file)
    cand_emb = get_embedding(candidate.file)

    if ref_emb is None or cand_emb is None:
        return JSONResponse(content={"match": False, "reason": "no_face_detected"})

    # Cosine similarity
    score = float(np.dot(ref_emb, cand_emb) /
                  (np.linalg.norm(ref_emb) * np.linalg.norm(cand_emb)))

    # Threshold
    MATCH_THRESHOLD = 0.45

    return {
        "match": score > MATCH_THRESHOLD,
        "score": score
    }
