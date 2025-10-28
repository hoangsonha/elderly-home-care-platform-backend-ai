# -*- coding: utf-8 -*-
"""
Simple script to test skills similarity between two input strings
"""

import sys
import codecs
from pathlib import Path

# Add the backend directory to the Python path
sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

# Fix for UnicodeEncodeError on Windows
if sys.stdout.encoding != 'utf-8':
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.detach())

from backend.app.algorithms.semantic_matcher import semantic_matcher

def run_skills_test(skill1: str, skill2: str, threshold: float):
    """
    Tests the similarity between two skills using the semantic matcher.
    """
    print("SKILLS SIMILARITY TEST")
    print("==================================================")
    print(f"'{skill1}' vs '{skill2}'")

    similarity = semantic_matcher.calculate_similarity(skill1, skill2)
    print(f"Similarity: {similarity:.3f} ({similarity*100:.1f}%)")
    print(f"Threshold: {threshold} ({threshold*100:.0f}%)")

    if similarity >= threshold:
        print("Result: ✅ PASSED")
    else:
        print("Result: ❌ FAILED")
    print("==================================================")

if __name__ == "__main__":
    # Customize these skills and threshold for your tests
    SKILL_1 = "kiểm tra huyết áp"
    SKILL_2 = "đo huyết áp"
    THRESHOLD = 0.8 # Threshold for PhoBERT v2 (strict matching)

    run_skills_test(SKILL_1, SKILL_2, THRESHOLD)
