package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.MatchCaregiverRequest;
import com.capstone_project.elderly_platform.dtos.response.MatchCaregiverResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class AIMatchingService {

    private final RestTemplate restTemplate;

    @Value("${ai.matching.service.url:http://ai_matching:8000}")
    private String aiServiceUrl;

    public AIMatchingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public MatchCaregiverResponse matchCaregivers(MatchCaregiverRequest request) {
        try {
            String url = aiServiceUrl + "/api/match-mobile";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<MatchCaregiverRequest> httpEntity = new HttpEntity<>(request, headers);

            log.info("Calling AI matching service: {}", url);

            ResponseEntity<MatchCaregiverResponse> response = restTemplate.postForEntity(
                    url,
                    httpEntity,
                    MatchCaregiverResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("AI matching service returned {} matches", response.getBody().getTotalMatches());
                return response.getBody();
            } else {
                log.error("AI matching service returned error status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to get response from AI matching service");
            }

        } catch (Exception e) {
            log.error("Error calling AI matching service: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling AI matching service: " + e.getMessage(), e);
        }
    }
}


