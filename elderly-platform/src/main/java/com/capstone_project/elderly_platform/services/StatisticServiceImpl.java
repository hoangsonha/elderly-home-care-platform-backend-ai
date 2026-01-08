package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.response.CareServiceStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.CaregiverPersonalStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.CaregiverStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerPersonalStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.UserStatisticsResponse;
import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.enums.EnumRoleType;
import com.capstone_project.elderly_platform.enums.EnumWorkTaskStatusType;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.pojos.CareService;
import com.capstone_project.elderly_platform.pojos.CareSeekerProfile;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.pojos.Payment;
import com.capstone_project.elderly_platform.pojos.PayoutBatch;
import com.capstone_project.elderly_platform.pojos.WorkSchedule;
import com.capstone_project.elderly_platform.pojos.WorkTask;
import com.capstone_project.elderly_platform.repositories.AccountRepository;
import com.capstone_project.elderly_platform.repositories.CareServiceRepository;
import com.capstone_project.elderly_platform.repositories.CareSeekerProfileRepository;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.repositories.PaymentRepository;
import com.capstone_project.elderly_platform.repositories.PayoutBatchRepository;
import com.capstone_project.elderly_platform.utils.AccountSpecification;
import com.capstone_project.elderly_platform.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

        private final AccountRepository accountRepository;
        private final CaregiverProfileRepository caregiverProfileRepository;
        private final CareSeekerProfileRepository careSeekerProfileRepository;
        private final CareServiceRepository careServiceRepository;
        private final PaymentRepository paymentRepository;
        private final PayoutBatchRepository payoutBatchRepository;
        private final ObjectMapper objectMapper;

        @Override
        public UserStatisticsResponse getUserStatistics(LocalDateTime startDate, LocalDateTime endDate) {
                log.info("Getting user statistics from {} to {}", startDate, endDate);

                // Base specification: not deleted and exclude admin
                Specification<Account> baseSpec = AccountSpecification.notDeleted()
                                .and(AccountSpecification.excludeAdminRole());

                // Add date range filter if provided
                if (startDate != null || endDate != null) {
                        baseSpec = baseSpec.and(AccountSpecification.filterByDateRange(startDate, endDate));
                }

                // Total registered users (excluding admin)
                Long totalRegisteredUsers = accountRepository.count(baseSpec);

                // Total unverified users (excluding admin)
                Specification<Account> unverifiedSpec = baseSpec.and(
                                (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("enabled"), false));
                Long totalUnverifiedUsers = accountRepository.count(unverifiedSpec);

                // Total caregivers
                Specification<Account> caregiverSpec = baseSpec
                                .and(AccountSpecification.filterByRole(EnumRoleType.ROLE_CAREGIVER));
                Long totalCaregivers = accountRepository.count(caregiverSpec);

                // Total care seekers
                Specification<Account> careSeekerSpec = baseSpec
                                .and(AccountSpecification.filterByRole(EnumRoleType.ROLE_CARE_SEEKER));
                Long totalCareSeekers = accountRepository.count(careSeekerSpec);

                // Unverified caregivers
                Specification<Account> unverifiedCaregiverSpec = caregiverSpec
                                .and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("enabled"),
                                                false));
                Long unverifiedCaregivers = accountRepository.count(unverifiedCaregiverSpec);

                // Unverified care seekers
                Specification<Account> unverifiedCareSeekerSpec = careSeekerSpec
                                .and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("enabled"),
                                                false));
                Long unverifiedCareSeekers = accountRepository.count(unverifiedCareSeekerSpec);

                log.info("Total registered users: {}, Total unverified users: {}, " +
                                "Total caregivers: {}, Total care seekers: {}, " +
                                "Unverified caregivers: {}, Unverified care seekers: {}",
                                totalRegisteredUsers, totalUnverifiedUsers,
                                totalCaregivers, totalCareSeekers,
                                unverifiedCaregivers, unverifiedCareSeekers);

                return UserStatisticsResponse.builder()
                                .totalRegisteredUsers(totalRegisteredUsers)
                                .totalUnverifiedUsers(totalUnverifiedUsers)
                                .totalCaregivers(totalCaregivers)
                                .totalCareSeekers(totalCareSeekers)
                                .unverifiedCaregivers(unverifiedCaregivers)
                                .unverifiedCareSeekers(unverifiedCareSeekers)
                                .build();
        }

        @Override
        public CaregiverStatisticsResponse getCaregiverStatistics() {
                log.info("Getting caregiver statistics");

                Long totalCaregivers = caregiverProfileRepository.countTotalCaregivers();
                Long pendingVerificationCaregivers = caregiverProfileRepository.countPendingVerificationCaregivers();

                log.info("Total caregivers: {}, Pending verification: {}",
                                totalCaregivers, pendingVerificationCaregivers);

                return CaregiverStatisticsResponse.builder()
                                .totalCaregivers(totalCaregivers)
                                .pendingVerificationCaregivers(pendingVerificationCaregivers)
                                .build();
        }

        @Override
        public CareServiceStatisticsResponse getCareServiceStatistics() {
                log.info("Getting care service statistics");

                // Count total care services (not deleted)
                Long totalCareServices = careServiceRepository.countTotalBookings();

                // Count by each status
                Map<String, Long> countByStatus = new java.util.HashMap<>();
                
                for (EnumCareServiceStatusType status : EnumCareServiceStatusType.values()) {
                        Long count = careServiceRepository.countByStatusAndDeletedFalse(status);
                        countByStatus.put(status.name(), count);
                }

                log.info("Total care services: {}, Count by status: {}", totalCareServices, countByStatus);

                return CareServiceStatisticsResponse.builder()
                                .totalCareServices(totalCareServices)
                                .countByStatus(countByStatus)
                                .build();
        }

        @Override
        public CaregiverPersonalStatisticsResponse getCaregiverPersonalStatistics() {
                UUID currentAccountId = SecurityUtils.getCurrentUserId();
                log.info("Getting personal statistics for caregiver with account ID: {}", currentAccountId);

                // Get caregiver profile
                CaregiverProfile caregiverProfile = caregiverProfileRepository
                                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);
                if (caregiverProfile == null) {
                        throw new ElementNotFoundException("Caregiver profile not found for current user");
                }

                // Get current month and year
                LocalDateTime now = LocalDateTime.now();
                int currentMonth = now.getMonthValue();
                int currentYear = now.getYear();

                // Get all care services for this caregiver (will be reused for task completion rate calculation)
                List<CareService> allCareServices = careServiceRepository
                                .findByCaregiverProfileAndDeletedIsFalse(caregiverProfile,
                                                org.springframework.data.domain.Sort.unsorted());

                // 1. Count total care services in current month
                List<CareService> careServicesThisMonth = allCareServices.stream()
                                .filter(cs -> cs.getWorkDate() != null
                                                && cs.getWorkDate().getYear() == currentYear
                                                && cs.getWorkDate().getMonthValue() == currentMonth)
                                .collect(java.util.stream.Collectors.toList());
                
                Long totalCareServicesThisMonth = (long) careServicesThisMonth.size();

                // 2. Get total earnings from PayoutBatch for current month
                Double totalEarningsThisMonth = 0.0;
                java.util.Optional<PayoutBatch> payoutBatchOpt = payoutBatchRepository
                                .findByCaregiverProfileAndYearAndMonth(
                                                caregiverProfile.getCaregiverProfileId(),
                                                currentYear,
                                                currentMonth);
                if (payoutBatchOpt.isPresent()) {
                        PayoutBatch payoutBatch = payoutBatchOpt.get();
                        totalEarningsThisMonth = payoutBatch.getTotalCaregiverEarnings() != null
                                        ? payoutBatch.getTotalCaregiverEarnings()
                                        : 0.0;
                }

                // 3. Get overall rating from profileData
                Double overallRating = 0.0;
                try {
                        String profileDataJson = caregiverProfile.getProfileData();
                        if (profileDataJson != null && !profileDataJson.isEmpty()) {
                                Map<String, Object> profileDataMap = objectMapper.readValue(profileDataJson,
                                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                                });
                                if (profileDataMap.containsKey("ratings_reviews")) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> ratingsReviews = (Map<String, Object>) profileDataMap
                                                        .get("ratings_reviews");
                                        if (ratingsReviews != null && ratingsReviews.containsKey("overall_rating")) {
                                                Object ratingObj = ratingsReviews.get("overall_rating");
                                                if (ratingObj instanceof Number) {
                                                        overallRating = ((Number) ratingObj).doubleValue();
                                                }
                                        }
                                }
                        }
                } catch (Exception e) {
                        log.warn("Failed to parse overall_rating from profileData: {}", e.getMessage());
                        overallRating = 0.0;
                }

                // 4. Calculate task completion rate
                // Reuse allCareServices already fetched above
                int totalTasks = 0;
                int completedTasks = 0;

                for (CareService careService : allCareServices) {
                        // Get work schedule for this care service
                        WorkSchedule workSchedule = careService.getWorkSchedule();
                        if (workSchedule != null) {
                                // Add totalTasks from workSchedule
                                if (workSchedule.getTotalTasks() != null) {
                                        totalTasks += workSchedule.getTotalTasks();
                                }

                                // Count completed tasks (status = DONE)
                                List<WorkTask> workTasks = workSchedule.getWorkTasks();
                                if (workTasks != null) {
                                        for (WorkTask task : workTasks) {
                                                if (task.getStatus() == EnumWorkTaskStatusType.DONE) {
                                                        completedTasks++;
                                                }
                                        }
                                }
                        }
                }

                // Calculate completion rate percentage
                Double taskCompletionRate = 0.0;
                if (totalTasks > 0) {
                        taskCompletionRate = (completedTasks * 100.0) / totalTasks;
                }

                log.info(
                                "Caregiver personal statistics - Care services this month: {}, Earnings: {}, Rating: {}, Task completion: {}%",
                                totalCareServicesThisMonth, totalEarningsThisMonth, overallRating,
                                taskCompletionRate);

                return CaregiverPersonalStatisticsResponse.builder()
                                .totalCareServicesThisMonth(totalCareServicesThisMonth)
                                .totalEarningsThisMonth(totalEarningsThisMonth)
                                .overallRating(overallRating)
                                .taskCompletionRate(taskCompletionRate)
                                .build();
        }

        @Override
        public CareSeekerPersonalStatisticsResponse getCareSeekerPersonalStatistics() {
                UUID currentAccountId = SecurityUtils.getCurrentUserId();
                log.info("Getting personal statistics for care seeker with account ID: {}", currentAccountId);

                // Get care seeker profile
                CareSeekerProfile careSeekerProfile = careSeekerProfileRepository
                                .findByAccount_AccountIdAndDeletedIsFalse(currentAccountId);
                if (careSeekerProfile == null) {
                        throw new ElementNotFoundException("Care seeker profile not found for current user");
                }

                // Get current month and year
                LocalDateTime now = LocalDateTime.now();
                int currentMonth = now.getMonthValue();
                int currentYear = now.getYear();

                // 1. Total elderly profiles
                List<com.capstone_project.elderly_platform.pojos.ElderlyProfile> elderlyProfiles = 
                        careSeekerProfile.getElderlyProfiles();
                Long totalElderlyProfiles = 0L;
                if (elderlyProfiles != null) {
                        totalElderlyProfiles = elderlyProfiles.stream()
                                .filter(e -> !e.isDeleted())
                                .count();
                }

                // Get all care services for this care seeker
                List<CareService> allCareServices = careServiceRepository
                                .findByCareSeekerProfileAndDeletedIsFalse(careSeekerProfile,
                                                org.springframework.data.domain.Sort.unsorted());

                // 2. Count total care services in current month
                List<CareService> careServicesThisMonth = allCareServices.stream()
                                .filter(cs -> cs.getWorkDate() != null
                                                && cs.getWorkDate().getYear() == currentYear
                                                && cs.getWorkDate().getMonthValue() == currentMonth)
                                .collect(java.util.stream.Collectors.toList());
                
                Long totalCareServicesThisMonth = (long) careServicesThisMonth.size();

                // 3. Total spending this month (from payments with status SUCCESS in current month)
                Double totalSpendingThisMonth = 0.0;
                List<Payment> allPayments = paymentRepository.findAll().stream()
                                .filter(p -> p.getSeekerProfile() != null
                                        && p.getSeekerProfile().getCareSeekerProfileId()
                                                .equals(careSeekerProfile.getCareSeekerProfileId())
                                        && p.getStatus() == com.capstone_project.elderly_platform.enums.EnumPaymentStatusType.SUCCESS
                                        && p.getPaidAt() != null
                                        && p.getPaidAt().getYear() == currentYear
                                        && p.getPaidAt().getMonthValue() == currentMonth)
                                .collect(java.util.stream.Collectors.toList());
                
                for (Payment payment : allPayments) {
                        if (payment.getAmount() != null) {
                                totalSpendingThisMonth += payment.getAmount();
                        }
                }

                // 4. Total completed bookings (care-services with status COMPLETED)
                List<CareService> completedCareServices = allCareServices.stream()
                                .filter(cs -> cs.getStatus() == EnumCareServiceStatusType.COMPLETED)
                                .collect(java.util.stream.Collectors.toList());
                Long totalCompletedBookings = (long) completedCareServices.size();

                // 5. Total in progress services (care-services with status IN_PROGRESS)
                List<CareService> inProgressCareServices = allCareServices.stream()
                                .filter(cs -> cs.getStatus() == EnumCareServiceStatusType.IN_PROGRESS)
                                .collect(java.util.stream.Collectors.toList());
                Long totalInProgressServices = (long) inProgressCareServices.size();

                log.info(
                                "Care seeker personal statistics - Elderly profiles: {}, Care services this month: {}, Spending: {}, Completed: {}, In progress: {}",
                                totalElderlyProfiles, totalCareServicesThisMonth, totalSpendingThisMonth,
                                totalCompletedBookings, totalInProgressServices);

                return CareSeekerPersonalStatisticsResponse.builder()
                                .totalElderlyProfiles(totalElderlyProfiles)
                                .totalCareServicesThisMonth(totalCareServicesThisMonth)
                                .totalSpendingThisMonth(totalSpendingThisMonth)
                                .totalCompletedBookings(totalCompletedBookings)
                                .totalInProgressServices(totalInProgressServices)
                                .build();
        }
}
