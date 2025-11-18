package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.pojos.Rating;

import java.util.List;
import java.util.UUID;

public interface RatingService {
    Rating createRating(Rating rating);
    List<Rating> getRatingsByServiceTask(UUID serviceTaskId);
    List<Rating> getSystemRatings();
    List<Rating> getComplaintRatings();

    // Thêm method
    Account getAccountById(UUID accountId);
}
