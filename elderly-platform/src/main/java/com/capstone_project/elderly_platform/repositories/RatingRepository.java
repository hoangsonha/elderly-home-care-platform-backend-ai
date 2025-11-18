package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    List<Rating> findByServiceTask_ServiceTaskId(UUID serviceTaskId);
    List<Rating> findBySystemFeedbackTrue();
    List<Rating> findByComplaintFeedbackTrue();
}
