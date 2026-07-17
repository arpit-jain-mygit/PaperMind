from fastapi import FastAPI, Depends
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
import os

from database import init_db, verify_db_connection, get_db
from vector_db import init_qdrant, verify_qdrant_connection

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

# Initialize databases on startup
@app.on_event("startup")
async def startup_event():
    """Initialize database and vector DB on app startup"""
    print("\n" + "="*60)
    print("PaperMind API - Startup Initialization")
    print("="*60)

    # Initialize PostgreSQL
    print("\n📊 PostgreSQL Setup:")
    init_db()
    db_ok = verify_db_connection()

    # Initialize Qdrant
    print("\n🗄️ Qdrant Vector DB Setup:")
    qdrant_ok = init_qdrant()
    verify_qdrant_connection()

    if db_ok and qdrant_ok:
        print("\n" + "="*60)
        print("✅ All systems initialized successfully")
        print("="*60 + "\n")
    else:
        print("\n" + "="*60)
        print("⚠️ Some initialization steps failed")
        print("="*60 + "\n")


@app.get("/health")
async def health(db: Session = Depends(get_db)):
    """Health check endpoint"""
    try:
        # Check database
        db.execute("SELECT 1")
        db_status = "healthy"
    except Exception as e:
        db_status = f"error: {str(e)}"

    return {
        "status": "healthy",
        "service": "papermind-api",
        "version": "1.0.0",
        "database": db_status
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
async def upload_document(db: Session = Depends(get_db)):
    """Document upload endpoint"""
    return {
        "status": "ready",
        "message": "Document upload endpoint initialized"
    }


@app.post("/api/query")
async def query_documents(db: Session = Depends(get_db)):
    """Query documents endpoint"""
    return {
        "status": "ready",
        "message": "Query endpoint initialized"
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
