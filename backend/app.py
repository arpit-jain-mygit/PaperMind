from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import os

app = FastAPI(
    title="PaperMind API",
    description="Personal Document AI System",
    version="1.0.0"
)

# CORS Configuration
cors_origin = os.getenv("CORS_ORIGIN", "http://localhost:3000")
app.add_middleware(
    CORSMiddleware,
    allow_origins=[cors_origin, "http://localhost:3000", "http://localhost:4200"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
async def health():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "papermind-api",
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "PaperMind API",
        "docs": "/docs",
        "health": "/health"
    }

@app.post("/api/documents/upload")
async def upload_document():
    """Document upload endpoint"""
    return {"status": "ready"}

@app.post("/api/query")
async def query_documents():
    """Query documents endpoint"""
    return {"status": "ready"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
