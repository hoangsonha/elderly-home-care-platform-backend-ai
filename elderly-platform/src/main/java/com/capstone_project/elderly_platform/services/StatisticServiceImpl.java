package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.response.CareServiceStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.CaregiverStatisticsResponse;
import com.capstone_project.elderly_platform.dtos.response.UserStatisticsResponse;
import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.enums.EnumRoleType;
import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.repositories.AccountRepository;
import com.capstone_project.elderly_platform.repositories.CareServiceRepository;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.utils.AccountSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

        private final AccountRepository accountRepository;
        private final CaregiverProfileRepository caregiverProfileRepository;
        private final CareServiceRepository careServiceRepository;

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
}
