import os
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct

# Qdrant configuration
QDRANT_URL = os.getenv("QDRANT_URL", "http://localhost:6333")
QDRANT_API_KEY = os.getenv("QDRANT_API_KEY", None)
COLLECTION_NAME = "papermind-documents"
VECTOR_SIZE = 1536  # OpenAI Ada-3 embedding size


def get_qdrant_client() -> QdrantClient:
    """Get Qdrant client instance"""
    if QDRANT_API_KEY:
        return QdrantClient(url=QDRANT_URL, api_key=QDRANT_API_KEY)
    else:
        return QdrantClient(url=QDRANT_URL)


def init_qdrant():
    """Initialize Qdrant collection for document embeddings"""
    try:
        client = get_qdrant_client()

        # Check if collection exists
        try:
            client.get_collection(COLLECTION_NAME)
            print(f"✅ Qdrant collection '{COLLECTION_NAME}' already exists")
            return True
        except Exception:
            # Collection doesn't exist, create it
            print(f"Creating Qdrant collection '{COLLECTION_NAME}'...")

            client.create_collection(
                collection_name=COLLECTION_NAME,
                vectors_config=VectorParams(
                    size=VECTOR_SIZE,
                    distance=Distance.COSINE  # Cosine similarity for semantic search
                ),
            )

            print(f"✅ Qdrant collection '{COLLECTION_NAME}' created successfully")
            print(f"   - Vector size: {VECTOR_SIZE}")
            print(f"   - Distance metric: COSINE")
            return True

    except Exception as e:
        print(f"❌ Failed to initialize Qdrant: {e}")
        return False


def verify_qdrant_connection() -> bool:
    """Verify Qdrant connection works"""
    try:
        client = get_qdrant_client()
        info = client.get_collection(COLLECTION_NAME)
        print(f"✅ Qdrant connection verified")
        print(f"   - Collection: {COLLECTION_NAME}")
        print(f"   - Vectors: {info.points_count}")
        return True
    except Exception as e:
        print(f"❌ Qdrant connection failed: {e}")
        return False


def search_documents(query_embedding: list, top_k: int = 5, user_id: str = None) -> list:
    """
    Search for similar documents in Qdrant

    Args:
        query_embedding: Vector embedding of the query
        top_k: Number of results to return
        user_id: Filter by user (optional)

    Returns:
        List of matching documents with scores
    """
    try:
        client = get_qdrant_client()

        results = client.search(
            collection_name=COLLECTION_NAME,
            query_vector=query_embedding,
            limit=top_k,
            query_filter=None if not user_id else {
                "filter": {
                    "must": [
                        {
                            "key": "user_id",
                            "match": {"value": user_id}
                        }
                    ]
                }
            }
        )

        return results
    except Exception as e:
        print(f"❌ Error searching Qdrant: {e}")
        return []


def store_embedding(
    doc_id: str,
    user_id: str,
    vector: list,
    metadata: dict,
    qdrant_id: str
) -> bool:
    """
    Store embedding in Qdrant

    Args:
        doc_id: Document ID from PostgreSQL
        user_id: User ID for filtering
        vector: Embedding vector
        metadata: Document metadata
        qdrant_id: ID to use in Qdrant

    Returns:
        Success status
    """
    try:
        client = get_qdrant_client()

        point = PointStruct(
            id=hash(qdrant_id) % (10 ** 8),  # Convert to positive int
            vector=vector,
            payload={
                "doc_id": doc_id,
                "user_id": user_id,
                "qdrant_id": qdrant_id,
                **metadata
            }
        )

        client.upsert(
            collection_name=COLLECTION_NAME,
            points=[point]
        )

        return True
    except Exception as e:
        print(f"❌ Error storing embedding: {e}")
        return False
