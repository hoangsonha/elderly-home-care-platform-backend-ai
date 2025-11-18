package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.pojos.Rating;
import com.capstone_project.elderly_platform.repositories.AccountRepository;
import com.capstone_project.elderly_platform.repositories.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final AccountRepository accountRepository;

    @Override
    public Rating createRating(Rating rating) {
        if (rating.getScore() < 1 || rating.getScore() > 5) {
            throw new IllegalArgumentException("Score phải từ 1 đến 5");
        }
        return ratingRepository.save(rating);
    }

    @Override
    public List<Rating> getRatingsByServiceTask(UUID serviceTaskId) {
        return ratingRepository.findByServiceTask_ServiceTaskId(serviceTaskId);
    }

    @Override
    public List<Rating> getSystemRatings() {
        return ratingRepository.findBySystemFeedbackTrue();
    }

    @Override
    public List<Rating> getComplaintRatings() {
        return ratingRepository.findByComplaintFeedbackTrue();
    }

    public Account getAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account không tồn tại"));
    }
}
