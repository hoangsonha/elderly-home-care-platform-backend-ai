"""
Time utilities for checking availability overlaps.
"""

from typing import List, Dict


def time_to_minutes(time_str: str) -> int:
    """
    Convert time string to minutes since midnight.
    
    Args:
        time_str: Time in format "HH:MM" (e.g., "08:00", "14:30")
    
    Returns:
        Minutes since midnight (e.g., "08:00" -> 480)
    
    Example:
        >>> time_to_minutes("08:00")
        480
        >>> time_to_minutes("14:30")
        870
    """
    hours, minutes = map(int, time_str.split(':'))
    return hours * 60 + minutes


def has_time_overlap(req_slots: List[Dict], cg_availability: Dict) -> bool:
    """
    Check if ALL requested time slots are available in caregiver's schedule.
    
    Args:
        req_slots: List of requested slots
            [{"day": "monday", "start": "08:00", "end": "12:00"}, ...]
        cg_availability: Caregiver's availability schedule
            {"monday": [{"start": "08:00", "end": "18:00"}], ...}
    
    Returns:
        True if ALL request slots fit within caregiver's availability
    
    Example:
        >>> req = [{"day": "monday", "start": "08:00", "end": "12:00"}]
        >>> cg = {"monday": [{"start": "08:00", "end": "18:00"}]}
        >>> has_time_overlap(req, cg)
        True
    """
    for req_slot in req_slots:
        day = req_slot['day']
        req_start = time_to_minutes(req_slot['start'])
        req_end = time_to_minutes(req_slot['end'])
        
        # Get caregiver's slots for this day
        cg_slots = cg_availability.get(day, [])
        
        if not cg_slots:
            return False
        
        # Check if request slot fits completely within any caregiver slot
        slot_fits = False
        for cg_slot in cg_slots:
            cg_start = time_to_minutes(cg_slot['start'])
            cg_end = time_to_minutes(cg_slot['end'])
            
            # Request must fit completely within caregiver slot
            if req_start >= cg_start and req_end <= cg_end:
                slot_fits = True
                break
        
        if not slot_fits:
            return False
    
    return True


def calculate_time_overlap_ratio(req_slots: List[Dict], cg_availability: Dict) -> float:
    """
    Calculate how well the caregiver's availability matches the request.
    
    Args:
        req_slots: Requested time slots
        cg_availability: Caregiver's availability
    
    Returns:
        Ratio between 0.0 and 1.0
        1.0 = perfect match (all slots fit)
        0.0 = no match
    
    Note:
        Current implementation is binary (0.0 or 1.0).
        Can be enhanced to calculate partial overlaps.
    """
    if not has_time_overlap(req_slots, cg_availability):
        return 0.0
    
    # If all slots fit, return perfect match
    # TODO: Can be enhanced to calculate partial overlap percentage
    return 1.0
