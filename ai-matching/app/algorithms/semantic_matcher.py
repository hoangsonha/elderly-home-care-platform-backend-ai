"""
PhoBERT Semantic Matching Algorithm
Sử dụng PhoBERT để tính semantic similarity cho tiếng Việt
"""

import numpy as np
from typing import List, Dict, Optional
import logging

# Try to import PhoBERT dependencies
try:
    from transformers import AutoTokenizer, AutoModel
    import torch
    import torch.nn.functional as F
    PHOBERT_AVAILABLE = True
except ImportError:
    PHOBERT_AVAILABLE = False
    logging.warning("PhoBERT dependencies not available. Install: pip install transformers torch")


class PhoBERTSemanticMatcher:
    """
    Semantic matching sử dụng PhoBERT cho tiếng Việt
    """
    
    def __init__(self, model_name: str = "vinai/phobert-base-v2"):
        """
        Initialize PhoBERT model
        
        Args:
            model_name: PhoBERT model name from Hugging Face
        """
        self.model_name = model_name
        self.tokenizer = None
        self.model = None
        self.device = None
        
        if PHOBERT_AVAILABLE:
            self._load_model()
        else:
            logging.error("PhoBERT not available. Please install dependencies.")
    
    def _load_model(self):
        """Load PhoBERT model and tokenizer"""
        try:
            logging.info(f"Loading PhoBERT model: {self.model_name}")
            
            # Load tokenizer
            self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)
            
            # Load model
            self.model = AutoModel.from_pretrained(self.model_name)
            
            # Set device (GPU if available, otherwise CPU)
            self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
            self.model.to(self.device)
            self.model.eval()
            
            logging.info(f"PhoBERT loaded successfully on {self.device}")
            
        except Exception as e:
            logging.error(f"Failed to load PhoBERT: {e}")
            self.tokenizer = None
            self.model = None
    
    def _get_embeddings(self, texts: List[str]) -> np.ndarray:
        """
        Get embeddings for a list of texts
        
        Args:
            texts: List of text strings
            
        Returns:
            numpy array of embeddings
        """
        if not self.model or not self.tokenizer:
            raise RuntimeError("PhoBERT model not loaded")
        
        embeddings = []
        
        with torch.no_grad():
            for text in texts:
                # Tokenize text
                inputs = self.tokenizer(
                    text,
                    return_tensors="pt",
                    padding=True,
                    truncation=True,
                    max_length=512
                ).to(self.device)
                
                # Get model outputs
                outputs = self.model(**inputs)
                
                # Use [CLS] token embedding (first token)
                embedding = outputs.last_hidden_state[:, 0, :].cpu().numpy()
                embeddings.append(embedding[0])
        
        return np.array(embeddings)
    
    def calculate_similarity(self, text1: str, text2: str) -> float:
        """
        Calculate semantic similarity between two texts using PhoBERT
        
        Args:
            text1: First text
            text2: Second text
            
        Returns:
            Similarity score between 0 and 1
        """
        if not self.model or not self.tokenizer:
            # Fallback to basic similarity if PhoBERT not available
            return self._fallback_similarity(text1, text2)
        
        try:
            # Normalize both texts
            norm_text1 = normalize_vietnamese_text(text1)
            norm_text2 = normalize_vietnamese_text(text2)
            
            # Check for exact match after normalization
            if norm_text1 == norm_text2:
                return 1.0
            
            # Get embeddings for both texts
            embeddings = self._get_embeddings([norm_text1, norm_text2])
            
            # Calculate cosine similarity
            similarity = self._cosine_similarity(embeddings[0], embeddings[1])
            
            return float(similarity)
            
        except Exception as e:
            logging.error(f"Error calculating PhoBERT similarity: {e}")
            return self._fallback_similarity(text1, text2)
    
    def calculate_batch_similarity(self, query_text: str, candidate_texts: List[str]) -> List[float]:
        """
        Calculate similarity between query and multiple candidates
        
        Args:
            query_text: Query text
            candidate_texts: List of candidate texts
            
        Returns:
            List of similarity scores
        """
        if not self.model or not self.tokenizer:
            # Fallback to basic similarity
            return [self._fallback_similarity(query_text, text) for text in candidate_texts]
        
        try:
            # Get embeddings for all texts
            all_texts = [query_text] + candidate_texts
            embeddings = self._get_embeddings(all_texts)
            
            query_embedding = embeddings[0]
            similarities = []
            
            for i in range(1, len(embeddings)):
                similarity = self._cosine_similarity(query_embedding, embeddings[i])
                similarities.append(float(similarity))
            
            return similarities
            
        except Exception as e:
            logging.error(f"Error calculating batch similarity: {e}")
            return [self._fallback_similarity(query_text, text) for text in candidate_texts]
    
    def _cosine_similarity(self, vec1: np.ndarray, vec2: np.ndarray) -> float:
        """Calculate cosine similarity between two vectors"""
        dot_product = np.dot(vec1, vec2)
        norm1 = np.linalg.norm(vec1)
        norm2 = np.linalg.norm(vec2)
        
        if norm1 == 0 or norm2 == 0:
            return 0.0
        
        similarity = dot_product / (norm1 * norm2)
        return max(0.0, min(1.0, similarity))  # Clamp between 0 and 1
    
    def _fallback_similarity(self, text1: str, text2: str) -> float:
        """
        Fallback similarity calculation using basic methods
        """
        # Simple word overlap similarity
        words1 = set(text1.lower().split())
        words2 = set(text2.lower().split())
        
        if not words1 or not words2:
            return 0.0
        
        intersection = len(words1.intersection(words2))
        union = len(words1.union(words2))
        
        return intersection / union if union > 0 else 0.0
    
    def is_available(self) -> bool:
        """Check if PhoBERT is available and loaded"""
        return PHOBERT_AVAILABLE and self.model is not None and self.tokenizer is not None
    
    def get_model_info(self) -> Dict:
        """Get information about the loaded model"""
        return {
            "model_name": self.model_name,
            "available": self.is_available(),
            "device": str(self.device) if self.device else None,
            "dependencies_installed": PHOBERT_AVAILABLE
        }


class SemanticMatcherWithCache:
    """
    Semantic matcher with caching for better performance
    """
    
    # def __init__(self, model_name: str = "vinai/phobert-base-v2"):
    def __init__(self, model_name: str = "vinai/phobert-base"):
        self.matcher = PhoBERTSemanticMatcher(model_name)
        self.embedding_cache = {}
        self.similarity_cache = {}
        self.cache_hits = 0
        self.cache_misses = 0
    
    def calculate_similarity(self, text1: str, text2: str) -> float:
        """
        Calculate similarity with caching
        """
        # Create cache key
        cache_key = tuple(sorted([text1, text2]))
        
        # Check cache first
        if cache_key in self.similarity_cache:
            self.cache_hits += 1
            return self.similarity_cache[cache_key]
        
        # Calculate similarity
        similarity = self.matcher.calculate_similarity(text1, text2)
        
        # Cache result
        self.similarity_cache[cache_key] = similarity
        self.cache_misses += 1
        
        return similarity
    
    def get_cache_stats(self) -> Dict:
        """Get cache statistics"""
        total_requests = self.cache_hits + self.cache_misses
        hit_rate = self.cache_hits / total_requests if total_requests > 0 else 0
        
        return {
            "cache_hits": self.cache_hits,
            "cache_misses": self.cache_misses,
            "hit_rate": hit_rate,
            "total_cached_similarities": len(self.similarity_cache)
        }
    
    def clear_cache(self):
        """Clear all caches"""
        self.embedding_cache.clear()
        self.similarity_cache.clear()
        self.cache_hits = 0
        self.cache_misses = 0
    
    def is_available(self) -> bool:
        """Check if matcher is available"""
        return self.matcher.is_available()


# Global semantic matcher instance
semantic_matcher = SemanticMatcherWithCache()

# Compatibility functions for existing code
def normalize_vietnamese_text(text: str) -> str:
    """
    Normalize Vietnamese text with diacritics handling
    """
    if not text:
        return ""
    
    import unicodedata
    import re
    
    # Convert to lowercase
    text = text.lower().strip()
    
    # Normalize to NFD and remove diacritics
    normalized = unicodedata.normalize('NFD', text)
    without_diacritics = ''.join(
        char for char in normalized 
        if unicodedata.category(char) != 'Mn'
    )
    
    # Remove extra spaces
    without_diacritics = re.sub(r'\s+', ' ', without_diacritics).strip()
    
    return without_diacritics

def normalize_request_skills(request: Dict) -> Dict:
    """
    Normalize Vietnamese skills in a request
    """
    if 'skills' not in request:
        return request
    
    skills = request['skills']
    
    # Normalize required_skills
    if 'required_skills' in skills:
        skills['required_skills'] = [normalize_vietnamese_text(skill) for skill in skills['required_skills']]
    
    # Normalize priority_skills
    if 'priority_skills' in skills:
        skills['priority_skills'] = [normalize_vietnamese_text(skill) for skill in skills['priority_skills']]
    
    return request

def normalize_caregiver_skills(caregiver: Dict) -> Dict:
    """
    Normalize Vietnamese skills in a caregiver
    """
    if 'skills' not in caregiver:
        return caregiver
    
    skills = caregiver['skills']
    normalized_skills = []
    
    for skill in skills:
        if isinstance(skill, dict):
            skill_name = skill.get('name', '')
            # Normalize skill name
            normalized_name = normalize_vietnamese_text(skill_name)
            
            # Create new skill object with normalized name
            new_skill = skill.copy()
            new_skill['name'] = normalized_name
            normalized_skills.append(new_skill)
        else:
            # If skill is just a string
            normalized_name = normalize_vietnamese_text(skill)
            normalized_skills.append(normalized_name)
    
    caregiver['skills'] = normalized_skills
    return caregiver

