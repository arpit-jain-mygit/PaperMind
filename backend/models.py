from sqlalchemy import Column, String, DateTime, LargeBinary, JSON, Float, Integer, Boolean
from sqlalchemy.ext.declarative import declarative_base
from datetime import datetime
import uuid

Base = declarative_base()

class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    email = Column(String, unique=True, index=True)
    encrypted_key = Column(LargeBinary)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class Document(Base):
    __tablename__ = "documents"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, index=True)
    filename = Column(String)
    doc_type = Column(String)  # passport, DL, PAN, Aadhar, ITR, Form16, salary_slip, etc
    file_size = Column(Integer)
    encrypted_file_path = Column(String)  # S3/storage path
    encrypted_metadata = Column(LargeBinary)  # Encrypted document metadata
    upload_status = Column(String, default="pending")  # pending, processing, completed, failed
    error_message = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, index=True)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class Extraction(Base):
    __tablename__ = "extractions"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    doc_id = Column(String, index=True)
    user_id = Column(String, index=True)
    doc_type = Column(String)
    extracted_data = Column(JSON)  # Structured extracted fields
    raw_text = Column(String)  # Raw OCR text (encrypted in production)
    confidence_score = Column(Float)  # 0-1 confidence in extraction
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class Embedding(Base):
    __tablename__ = "embeddings"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    doc_id = Column(String, index=True)
    user_id = Column(String, index=True)
    qdrant_id = Column(String)  # Reference to Qdrant vector DB
    doc_type = Column(String)
    text_chunk = Column(String)  # Text that was embedded
    embedding_model = Column(String, default="openai-ada-3")  # Which model created embedding
    created_at = Column(DateTime, default=datetime.utcnow)


class ChatHistory(Base):
    __tablename__ = "chat_history"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, index=True)
    message = Column(String)  # User question
    response = Column(String)  # LLM response
    cited_docs = Column(JSON)  # List of document IDs used for answer
    model_used = Column(String, default="gpt-4o-mini")
    tokens_used = Column(Integer)
    cost = Column(Float)
    created_at = Column(DateTime, default=datetime.utcnow, index=True)
