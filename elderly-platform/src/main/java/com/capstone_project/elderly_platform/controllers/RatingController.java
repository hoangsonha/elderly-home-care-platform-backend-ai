package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.RatingRequest;
import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.pojos.Rating;
import com.capstone_project.elderly_platform.pojos.ServiceTask;
import com.capstone_project.elderly_platform.repositories.ServiceTaskRepository;
import com.capstone_project.elderly_platform.services.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;
    private final ServiceTaskRepository serviceTaskRepository;

    // Tạo rating mới
    @PostMapping
    public ResponseEntity<Rating> createRating(@RequestBody RatingRequest request) {
        // Lấy account theo ID
        Account account = ratingService.getAccountById(request.getAccountId());

        // Lấy serviceTask nếu có
        ServiceTask task = null;
        if (request.getServiceTaskId() != null) {
            task = serviceTaskRepository.findById(request.getServiceTaskId())
                    .orElseThrow(() -> new RuntimeException("ServiceTask không tồn tại"));
        }

        // Build Rating object
        Rating rating = Rating.builder()
                .account(account)
                .serviceTask(task)
                .score(request.getScore())
                .comment(request.getComment())
                .systemFeedback(request.isSystemFeedback())
                .complaintFeedback(request.isComplaintFeedback())
                .build();

        Rating saved = ratingService.createRating(rating);
        return ResponseEntity.ok(saved);
    }

    // Lấy tất cả rating theo ServiceTask
    @GetMapping("/service-task/{serviceTaskId}")
    public ResponseEntity<List<Rating>> getRatingsByServiceTask(@PathVariable UUID serviceTaskId) {
        List<Rating> ratings = ratingService.getRatingsByServiceTask(serviceTaskId);
        return ResponseEntity.ok(ratings);
    }

    // Lấy tất cả rating hệ thống
    @GetMapping("/system")
    public ResponseEntity<List<Rating>> getSystemRatings() {
        List<Rating> ratings = ratingService.getSystemRatings();
        return ResponseEntity.ok(ratings);
    }

    // Lấy tất cả rating khiếu nại
    @GetMapping("/complaint")
    public ResponseEntity<List<Rating>> getComplaintRatings() {
        List<Rating> ratings = ratingService.getComplaintRatings();
        return ResponseEntity.ok(ratings);
    }
}
