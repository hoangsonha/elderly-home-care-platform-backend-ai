"""
Vietnamese Fuzzy Matching Algorithm
Sử dụng synonyms và similarity để so sánh tiếng Việt
"""

import unicodedata
import re
from difflib import SequenceMatcher


class VietnameseFuzzyMatcher:
    """
    Fuzzy matching cho tiếng Việt sử dụng synonyms và similarity
    """
    
    def __init__(self):
        # Vietnamese synonyms mapping
        self.synonyms = {
            # Medical terms
            "tiêm": ["chích", "injection", "tiêm thuốc"],
            "insulin": ["insulin", "insulin"],
            "đo": ["kiểm tra", "test", "measure"],
            "đường huyết": ["đường máu", "blood sugar", "glucose"],
            "đái tháo đường": ["tiểu đường", "diabetes", "đái đường"],
            "quản lý": ["quản trị", "manage", "management"],
            "thuốc": ["medication", "medicine", "drug"],
            "chăm sóc": ["care", "nursing", "tending"],
            "vết thương": ["wound", "injury", "cut"],
            "dấu hiệu sinh tồn": ["vital signs", "vital", "signs"],
            "nấu ăn": ["cooking", "meal preparation", "food preparation"],
            
            # Common variations
            "bệnh nhân": ["patient", "người bệnh", "người ốm"],
            "người già": ["elderly", "senior", "old person"],
            "cao tuổi": ["elderly", "senior", "old"],
            "hỗ trợ": ["support", "assist", "help"],
            "vệ sinh": ["hygiene", "cleanliness", "sanitation"],
            "đi lại": ["mobility", "movement", "walking"],
            "huyết áp": ["blood pressure", "BP"],
            "cao huyết áp": ["hypertension", "high blood pressure"],
            "đột quỵ": ["stroke", "cerebrovascular"],
            "phục hồi": ["recovery", "rehabilitation", "rehab"]
        }
    
    def normalize_text(self, text):
        """
        Chuẩn hóa text: lowercase, bỏ dấu, loại ký tự đặc biệt
        """
        if not text:
            return ""
            
        # Normalize to NFD and remove diacritics
        normalized = unicodedata.normalize('NFD', text.lower())
        without_diacritics = ''.join(
            char for char in normalized 
            if unicodedata.category(char) != 'Mn'
        )
        
        # Remove special characters, keep only letters, numbers, spaces
        cleaned = re.sub(r'[^a-zA-Z0-9\s]', '', without_diacritics)
        
        # Remove extra spaces
        cleaned = ' '.join(cleaned.split())
        
        return cleaned
    
    def expand_synonyms(self, text):
        """
        Mở rộng text với synonyms
        """
        normalized = self.normalize_text(text)
        words = normalized.split()
        
        expanded_variations = [normalized]  # Original text
        
        # Add variations with synonyms
        for word in words:
            if word in self.synonyms:
                for synonym in self.synonyms[word]:
                    variation = normalized.replace(word, synonym)
                    expanded_variations.append(variation)
        
        return expanded_variations
    
    def calculate_similarity(self, text1, text2):
        """
        Tính similarity giữa 2 text sử dụng multiple methods
        """
        # Normalize both texts
        norm1 = self.normalize_text(text1)
        norm2 = self.normalize_text(text2)
        
        # Method 1: Exact match
        if norm1 == norm2:
            return 1.0
        
        # Method 2: Sequence similarity
        sequence_sim = SequenceMatcher(None, norm1, norm2).ratio()
        
        # Method 3: Word overlap
        words1 = set(norm1.split())
        words2 = set(norm2.split())
        
        if not words1 or not words2:
            return 0.0
            
        word_overlap = len(words1.intersection(words2)) / len(words1.union(words2))
        
        # Method 4: Synonym matching
        expanded1 = self.expand_synonyms(text1)
        expanded2 = self.expand_synonyms(text2)
        
        synonym_sim = 0.0
        for exp1 in expanded1:
            for exp2 in expanded2:
                if exp1 == exp2:
                    synonym_sim = 1.0
                    break
            if synonym_sim == 1.0:
                break
        
        # Combine all methods
        final_score = max(sequence_sim, word_overlap, synonym_sim)
        
        return final_score


# Global fuzzy matcher instance
fuzzy_matcher = VietnameseFuzzyMatcher()


def normalize_vietnamese_text(text):
    """
    Normalize Vietnamese text by removing diacritics for comparison.
    
    Args:
        text: Vietnamese text string
        
    Returns:
        Normalized text without diacritics
    """
    return fuzzy_matcher.normalize_text(text)


def normalize_request_skills(request):
    """
    Normalize Vietnamese skills in a request by removing diacritics.
    
    Args:
        request: Care request dictionary with skills field
        
    Returns:
        Modified request with normalized skills
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


def normalize_caregiver_skills(caregiver):
    """
    Normalize Vietnamese skills in a caregiver by removing diacritics.
    
    Args:
        caregiver: Caregiver dictionary with skills field
        
    Returns:
        Modified caregiver with normalized skills
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
