"""
Elder Care Connect - AI Matching Service
FastAPI Application - Phase 1: Rule-based Matching
"""

import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api import match
from app.models.schemas import HealthResponse

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)

# Create FastAPI app
app = FastAPI(
    title="Elder Care Connect - AI Matching API",
    description="""
    ## AI-powered Caregiver Matching System
    
    ### Phase 1: Rule-based Matching
    
    Hệ thống matching sử dụng weighted scoring algorithm với:
    - **Hard Filters**: Care level, degree requirement, distance, time availability, gender preference
    - **Soft Scoring**: 8 features (credential, distance, time, rating, experience, price, trust)
    - **Weighted Sum**: Kết hợp điểm với trọng số tối ưu
    
    ### Features
    - **Accurate Matching**: 8 features với trọng số được tinh chỉnh
    - **Explainable**: Breakdown chi tiết điểm số từng feature
    - **Fast**: <100ms response time
    - **Scalable**: Ready cho Phase 2 (Semantic Matching)
    
    ### Tech Stack
    - FastAPI
    - Python 3.10+
    - NumPy (for calculations)
    
    ### Roadmap
    - [x] Phase 1: Rule-based (Current)
    - [ ] Phase 2: Semantic Matching với Sentence-BERT
    - [ ] Phase 3: Learning-to-Rank với user data
    """,
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS middleware cho React Native
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Trong production nên chỉ định cụ thể
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include API routers
app.include_router(match.router, prefix="/api", tags=["Matching"])


# Root endpoint
@app.get("/", tags=["Root"])
async def root():
    """Root endpoint - API information"""
    return {
        "message": "Elder Care Connect - AI Matching API",
        "version": "1.0.0",
        "phase": "Phase 1: Rule-based Matching",
        "docs": "/docs",
        "endpoints": {
            "health": "/health",
            "requests": "/api/requests",
            "caregivers": "/api/caregivers",
            "match": "/api/match"
        }
    }


# Health check
@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """
    Health check endpoint
    
    Returns API status
    """
    return HealthResponse(
        status="healthy",
        message="AI Matching Service is running"
    )


# Startup event
@app.on_event("startup")
async def startup_event():
    """
    Actions khi server khởi động
    """
    print("=" * 60)
    print("Elder Care Connect - AI Matching Service")
    print("=" * 60)
    print("Phase 1: Rule-based Matching")
    print("Swagger UI: http://localhost:8000/docs")
    print("ReDoc: http://localhost:8000/redoc")
    print("=" * 60)


# Shutdown event
@app.on_event("shutdown")
async def shutdown_event():
    """
    Actions khi server tắt
    """
    print("\n👋 Shutting down AI Matching Service...")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=True
    )
