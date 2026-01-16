package com.capstone_project.elderly_platform.initdb;

import com.capstone_project.elderly_platform.dtos.request.LocationRequest;
import com.capstone_project.elderly_platform.enums.EnumActivationStatusType;
import com.capstone_project.elderly_platform.enums.EnumGenderType;
import com.capstone_project.elderly_platform.enums.EnumRoleType;
import com.capstone_project.elderly_platform.enums.EnumServicePackageType;
import com.capstone_project.elderly_platform.enums.EnumSystemConfigKey;
import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.pojos.CareSeekerProfile;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.pojos.ElderlyProfile;
import com.capstone_project.elderly_platform.pojos.Role;
import com.capstone_project.elderly_platform.dtos.QualificationRequirements;
import com.capstone_project.elderly_platform.pojos.QualificationType;
import com.capstone_project.elderly_platform.pojos.ServicePackage;
import com.capstone_project.elderly_platform.pojos.ServiceTask;
import com.capstone_project.elderly_platform.pojos.SystemConfig;
import com.capstone_project.elderly_platform.repositories.AccountRepository;
import com.capstone_project.elderly_platform.repositories.CareSeekerProfileRepository;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.repositories.ElderlyProfileRepository;
import com.capstone_project.elderly_platform.repositories.RoleRepository;
import com.capstone_project.elderly_platform.repositories.ServicePackageRepository;
import com.capstone_project.elderly_platform.repositories.ServiceTaskRepository;
import com.capstone_project.elderly_platform.repositories.SystemConfigRepository;
import com.capstone_project.elderly_platform.repositories.QualificationTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInit implements CommandLineRunner {

        private final RoleRepository roleRepository;
        private final AccountRepository accountRepository;
        private final SystemConfigRepository systemConfigRepository;
        private final BCryptPasswordEncoder bCryptPasswordEncoder;
        private final CareSeekerProfileRepository careSeekerProfileRepository;
        private final CaregiverProfileRepository caregiverProfileRepository;
        private final ElderlyProfileRepository elderlyProfileRepository;
        private final ServicePackageRepository servicePackageRepository;
        private final ServiceTaskRepository serviceTaskRepository;
        private final QualificationTypeRepository qualificationTypeRepository;
        private final ObjectMapper objectMapper;

        @Override
        public void run(String... args) {
                initRoles();
                Account adminAccount = initAccounts();
                initSystemConfigs(adminAccount != null ? adminAccount.getAccountId() : null);
                initQualificationTypes();
                initProfiles();
                initServicePackages();
        }

        private void initRoles() {
                if (roleRepository.count() == 0) {
                        log.info("Initializing roles...");

                        Role adminRole = Role.builder()
                                        .roleName(EnumRoleType.ROLE_ADMIN)
                                        .description("Administrator role with full system access")
                                        .build();
                        roleRepository.save(adminRole);

                        Role seekerRole = Role.builder()
                                        .roleName(EnumRoleType.ROLE_CARE_SEEKER)
                                        .description("Care seeker role for family members seeking care services")
                                        .build();
                        roleRepository.save(seekerRole);

                        Role caregiverRole = Role.builder()
                                        .roleName(EnumRoleType.ROLE_CAREGIVER)
                                        .description("Caregiver role for service providers")
                                        .build();
                        roleRepository.save(caregiverRole);

                        log.info("Roles initialized successfully");
                }
        }

        private void initSystemConfigs(java.util.UUID adminAccountId) {
                if (systemConfigRepository.count() == 0) {
                        log.info("Initializing system configs...");

                        // Service Package - Minimum Advance Hours
                        createSystemConfig(
                                        EnumSystemConfigKey.SERVICE_PACKAGE_MINIMUM_ADVANCE_HOURS_BASIC,
                                        "12",
                                        "Gói Cơ Bản: Tối thiểu 12 giờ đặt trước",
                                        adminAccountId);

                        createSystemConfig(
                                        EnumSystemConfigKey.SERVICE_PACKAGE_MINIMUM_ADVANCE_HOURS_PROFESSIONAL,
                                        "24",
                                        "Gói Chuyên Nghiệp: Tối thiểu 24 giờ đặt trước",
                                        adminAccountId);

                        createSystemConfig(
                                        EnumSystemConfigKey.SERVICE_PACKAGE_MINIMUM_ADVANCE_HOURS_ADVANCED,
                                        "48",
                                        "Gói Nâng Cao: Tối thiểu 48 giờ đặt trước",
                                        adminAccountId);

                        // Caregiver Response Deadline Hours
                        createSystemConfig(
                                        EnumSystemConfigKey.CAREGIVER_RESPONSE_DEADLINE_3_DAYS_OR_MORE,
                                        "24",
                                        "Đặt trước ≥3 ngày: 24 giờ để caregiver phản hồi",
                                        adminAccountId);

                        createSystemConfig(
                                        EnumSystemConfigKey.CAREGIVER_RESPONSE_DEADLINE_1_TO_2_DAYS,
                                        "12",
                                        "Đặt trước 1-2 ngày: 12 giờ để caregiver phản hồi",
                                        adminAccountId);

                        createSystemConfig(
                                        EnumSystemConfigKey.CAREGIVER_RESPONSE_DEADLINE_LESS_THAN_24H,
                                        "6",
                                        "Đặt trước <24h: 6 giờ để caregiver phản hồi",
                                        adminAccountId);

                        // System Fee
                        createSystemConfig(
                                        EnumSystemConfigKey.SYSTEM_FEE_PERCENTAGE,
                                        "10",
                                        "Phí hệ thống: 10%",
                                        adminAccountId);

                        log.info("System configs initialized successfully");
                }
        }

        private void createSystemConfig(EnumSystemConfigKey key, String value, String description,
                        java.util.UUID changedByAccountId) {
                SystemConfig config = SystemConfig.builder()
                                .configKey(key)
                                .configValue(value)
                                .version(1)
                                .active(true)
                                .changedByAccountId(changedByAccountId)
                                .description(description)
                                .build();
                systemConfigRepository.save(config);
        }

        private Account initAccounts() {
                if (accountRepository.count() == 0) {
                        log.info("Initializing default accounts...");

                        // Fetch roles with safety check
                        Role adminRoleList = roleRepository.getRoleByRoleName(EnumRoleType.ROLE_ADMIN);
                        Role seekerRoleList = roleRepository.getRoleByRoleName(EnumRoleType.ROLE_CARE_SEEKER);
                        Role caregiverRoleList = roleRepository.getRoleByRoleName(EnumRoleType.ROLE_CAREGIVER);

                        // Admin account
                        Account adminAccount = Account.builder()
                                        .email("admin@elderlycare.com")
                                         .password(bCryptPasswordEncoder.encode("Admin@123"))
                                        .enabled(true)
                                        .nonLocked(true)
                                        .role(adminRoleList)
                                        .build();
                        adminAccount = accountRepository.save(adminAccount);
                        log.info("Admin account created: admin@elderlycare.com / Admin@123");

                        // Care Seeker account
                        Account seekerAccount = Account.builder()
                                        .email("seeker@elderlycare.com")
                                        .password(bCryptPasswordEncoder.encode("Seeker@123"))
                                        .enabled(true)
                                        .nonLocked(true)
                                        .role(seekerRoleList)
                                        .avatarUrl("https://example.com/avatar/seeker.jpg")
                                        .build();
                        accountRepository.save(seekerAccount);
                        log.info("Care Seeker account created: seeker@elderlycare.com / Seeker@123");

                        // Caregiver account 1
                        Account caregiverAccount1 = Account.builder()
                                        .email("caregiver@elderlycare.com")
                                        .password(bCryptPasswordEncoder.encode("Caregiver@123"))
                                        .enabled(true)
                                        .nonLocked(true)
                                        .role(caregiverRoleList)
                                        .avatarUrl("https://example.com/avatar/caregiver1.jpg")
                                        .build();
                        accountRepository.save(caregiverAccount1);
                        log.info("Caregiver account 1 created: caregiver@elderlycare.com / Caregiver@123");

                        // Caregiver account 2
                        Account caregiverAccount2 = Account.builder()
                                        .email("caregiver2@elderlycare.com")
                                        .password(bCryptPasswordEncoder.encode("Caregiver2@123"))
                                        .enabled(true)
                                        .nonLocked(true)
                                        .role(caregiverRoleList)
                                        .avatarUrl("https://example.com/avatar/caregiver2.jpg")
                                        .build();
                        accountRepository.save(caregiverAccount2);
                        log.info("Caregiver account 2 created: caregiver2@elderlycare.com / Caregiver2@123");

                        log.info("Default accounts initialized successfully");
                        return adminAccount;
                }
                // If accounts already exist, find admin account
                return accountRepository.findAll().stream()
                                .filter(account -> account.getRole() != null &&
                                                account.getRole().getRoleName() == EnumRoleType.ROLE_ADMIN)
                                .findFirst()
                                .orElse(null);
        }

        private void initProfiles() {
                if (careSeekerProfileRepository.count() == 0) {
                        log.info("Initializing sample profiles...");

                        // Get accounts
                        Account seekerAccount = accountRepository.findAll().stream()
                                        .filter(account -> account.getRole() != null &&
                                                        account.getRole()
                                                                        .getRoleName() == EnumRoleType.ROLE_CARE_SEEKER)
                                        .findFirst()
                                        .orElse(null);

                        Account caregiverAccount1 = accountRepository.findAll().stream()
                                        .filter(account -> account.getEmail().equals("caregiver@elderlycare.com"))
                                        .findFirst()
                                        .orElse(null);

                        Account caregiverAccount2 = accountRepository.findAll().stream()
                                        .filter(account -> account.getEmail().equals("caregiver2@elderlycare.com"))
                                        .findFirst()
                                        .orElse(null);

                        if (seekerAccount == null || caregiverAccount1 == null || caregiverAccount2 == null) {
                                log.warn("Cannot create profiles: required accounts not found");
                                return;
                        }

                        // Create CareSeekerProfile
                        String seekerLocationJson = createLocationJson("123 Đường Nguyễn Văn A, Quận 1, TP.HCM",
                                        10.762622, 106.660172);
                        
                        // Build profileData for Care Seeker
                        Map<String, Object> seekerProfileDataMap = new HashMap<>();
                        seekerProfileDataMap.put("preferredContactTime", "9:00-17:00");
                        String seekerProfileDataJson;
                        try {
                                seekerProfileDataJson = objectMapper.writeValueAsString(seekerProfileDataMap);
                        } catch (Exception e) {
                                log.error("Failed to create seeker profileData JSON", e);
                                seekerProfileDataJson = "{\"preferredContactTime\":\"9:00-17:00\"}";
                        }
                        
                        CareSeekerProfile careSeekerProfile = CareSeekerProfile.builder()
                                        .fullName("Nguyễn Văn Tìm")
                                        .phoneNumber("0901234567")
                                        .location(seekerLocationJson)
                                        .birthDate(LocalDate.of(1985, 5, 15))
                                        .gender(EnumGenderType.MALE)
                                        .profileData(seekerProfileDataJson)
                                        .account(seekerAccount)
                                        .build();
                        careSeekerProfile = careSeekerProfileRepository.save(careSeekerProfile);
                        log.info("CareSeekerProfile created: {}", careSeekerProfile.getFullName());

                        // Create CaregiverProfile 1
                        String caregiver1LocationJson = createLocationJson("456 Đường Lê Lợi, Quận 3, TP.HCM", 10.7769,
                                        106.7009);
                        
                        // Build profileData for Caregiver 1
                        Map<String, Object> caregiver1ProfileDataMap = new HashMap<>();
                        caregiver1ProfileDataMap.put("years_experience", 5);
                        caregiver1ProfileDataMap.put("citizen_id", "079123456789");
                        caregiver1ProfileDataMap.put("citizen_id_front_image_url", "https://example.com/citizen/front1.jpg");
                        caregiver1ProfileDataMap.put("citizen_id_back_image_url", "https://example.com/citizen/back1.jpg");
                        
                        // Free schedule - available all time
                        Map<String, Object> freeSchedule1 = new HashMap<>();
                        freeSchedule1.put("available_all_time", true);
                        caregiver1ProfileDataMap.put("free_schedule", freeSchedule1);
                        
                        // Max hours per week
                        caregiver1ProfileDataMap.put("max_hours_per_week", 40);
                        
                        // Preferences
                        Map<String, Object> preferences1 = new HashMap<>();
                        preferences1.put("preferred_health_status", "MODERATE");
                        Map<String, Object> agePreference1 = new HashMap<>();
                        agePreference1.put("min_age", 70);
                        agePreference1.put("max_age", 85);
                        preferences1.put("elderly_age_preference", agePreference1);
                        caregiver1ProfileDataMap.put("preferences", preferences1);
                        
                        // Ratings reviews
                        Map<String, Object> ratingsReviews1 = new HashMap<>();
                        ratingsReviews1.put("overall_rating", 0);
                        ratingsReviews1.put("total_reviews", 0);
                        Map<String, Integer> ratingBreakdown1 = new HashMap<>();
                        ratingBreakdown1.put("5_star", 0);
                        ratingBreakdown1.put("4_star", 0);
                        ratingBreakdown1.put("3_star", 0);
                        ratingBreakdown1.put("2_star", 0);
                        ratingBreakdown1.put("1_star", 0);
                        ratingsReviews1.put("rating_breakdown", ratingBreakdown1);
                        Map<String, Double> detailedRatingsBreakdown1 = new HashMap<>();
                        detailedRatingsBreakdown1.put("professionalism", 0.0);
                        detailedRatingsBreakdown1.put("attitude", 0.0);
                        detailedRatingsBreakdown1.put("punctuality", 0.0);
                        detailedRatingsBreakdown1.put("quality", 0.0);
                        ratingsReviews1.put("detailed_ratings_breakdown", detailedRatingsBreakdown1);
                        caregiver1ProfileDataMap.put("ratings_reviews", ratingsReviews1);
                        
                        String caregiver1ProfileDataJson;
                        try {
                                caregiver1ProfileDataJson = objectMapper.writeValueAsString(caregiver1ProfileDataMap);
                        } catch (Exception e) {
                                log.error("Failed to create caregiver1 profileData JSON", e);
                                caregiver1ProfileDataJson = "{}";
                        }
                        
                        CaregiverProfile caregiverProfile1 = CaregiverProfile.builder()
                                        .fullName("Trần Thị Chăm Sóc")
                                        .phoneNumber("0902345678")
                                        .location(caregiver1LocationJson)
                                        .bio("Kinh nghiệm 5 năm chăm sóc người cao tuổi, chuyên về bệnh Alzheimer và Parkinson")
                                        .isVerified(true)
                                        .birthDate(LocalDate.of(1990, 8, 20))
                                        .gender(EnumGenderType.FEMALE)
                                        .profileData(caregiver1ProfileDataJson)
                                        .account(caregiverAccount1)
                                        .build();
                        caregiverProfileRepository.save(caregiverProfile1);
                        log.info("CaregiverProfile 1 created: {}", caregiverProfile1.getFullName());

                        // Create CaregiverProfile 2
                        String caregiver2LocationJson = createLocationJson("789 Đường Võ Văn Tần, Quận 10, TP.HCM",
                                        10.7730, 106.6660);
                        
                        // Build profileData for Caregiver 2
                        Map<String, Object> caregiver2ProfileDataMap = new HashMap<>();
                        caregiver2ProfileDataMap.put("years_experience", 7);
                        caregiver2ProfileDataMap.put("citizen_id", "079987654321");
                        caregiver2ProfileDataMap.put("citizen_id_front_image_url", "https://example.com/citizen/front2.jpg");
                        caregiver2ProfileDataMap.put("citizen_id_back_image_url", "https://example.com/citizen/back2.jpg");
                        
                        // Free schedule - specific schedule with booked slots
                        Map<String, Object> freeSchedule2 = new HashMap<>();
                        List<Map<String, Object>> bookedSlots2 = new ArrayList<>();
                        // Example: booked slot on 2026-01-15 from 10:00 to 14:00
                        Map<String, Object> bookedSlot1 = new HashMap<>();
                        bookedSlot1.put("date", "2026-01-15");
                        bookedSlot1.put("start_time", "10:00");
                        bookedSlot1.put("end_time", "14:00");
                        bookedSlots2.add(bookedSlot1);
                        freeSchedule2.put("booked_slots", bookedSlots2);
                        caregiver2ProfileDataMap.put("free_schedule", freeSchedule2);
                        
                        // Max hours per week
                        caregiver2ProfileDataMap.put("max_hours_per_week", 48);
                        
                        // Preferences
                        Map<String, Object> preferences2 = new HashMap<>();
                        preferences2.put("preferred_health_status", "GOOD");
                        Map<String, Object> agePreference2 = new HashMap<>();
                        agePreference2.put("min_age", 65);
                        agePreference2.put("max_age", 90);
                        preferences2.put("elderly_age_preference", agePreference2);
                        caregiver2ProfileDataMap.put("preferences", preferences2);
                        
                        // Ratings reviews
                        Map<String, Object> ratingsReviews2 = new HashMap<>();
                        ratingsReviews2.put("overall_rating", 0);
                        ratingsReviews2.put("total_reviews", 0);
                        Map<String, Integer> ratingBreakdown2 = new HashMap<>();
                        ratingBreakdown2.put("5_star", 0);
                        ratingBreakdown2.put("4_star", 0);
                        ratingBreakdown2.put("3_star", 0);
                        ratingBreakdown2.put("2_star", 0);
                        ratingBreakdown2.put("1_star", 0);
                        ratingsReviews2.put("rating_breakdown", ratingBreakdown2);
                        Map<String, Double> detailedRatingsBreakdown2 = new HashMap<>();
                        detailedRatingsBreakdown2.put("professionalism", 0.0);
                        detailedRatingsBreakdown2.put("attitude", 0.0);
                        detailedRatingsBreakdown2.put("punctuality", 0.0);
                        detailedRatingsBreakdown2.put("quality", 0.0);
                        ratingsReviews2.put("detailed_ratings_breakdown", detailedRatingsBreakdown2);
                        caregiver2ProfileDataMap.put("ratings_reviews", ratingsReviews2);
                        
                        String caregiver2ProfileDataJson;
                        try {
                                caregiver2ProfileDataJson = objectMapper.writeValueAsString(caregiver2ProfileDataMap);
                        } catch (Exception e) {
                                log.error("Failed to create caregiver2 profileData JSON", e);
                                caregiver2ProfileDataJson = "{}";
                        }
                        
                        CaregiverProfile caregiverProfile2 = CaregiverProfile.builder()
                                        .fullName("Lê Văn Yêu Thương")
                                        .phoneNumber("0903456789")
                                        .location(caregiver2LocationJson)
                                        .bio("Chuyên viên vật lý trị liệu, có chứng chỉ chăm sóc người cao tuổi quốc tế")
                                        .isVerified(true)
                                        .birthDate(LocalDate.of(1988, 3, 10))
                                        .gender(EnumGenderType.MALE)
                                        .profileData(caregiver2ProfileDataJson)
                                        .account(caregiverAccount2)
                                        .build();
                        caregiverProfileRepository.save(caregiverProfile2);
                        log.info("CaregiverProfile 2 created: {}", caregiverProfile2.getFullName());

                        // Create ElderlyProfile 1
                        String elderly1LocationJson = createLocationJson("123 Đường Nguyễn Văn A, Quận 1, TP.HCM",
                                        10.762622, 106.660172);
                        ElderlyProfile elderlyProfile1 = ElderlyProfile.builder()
                                        .fullName("Nguyễn Thị Bà")
                                        .phoneNumber("0901111111")
                                        .birthDate(LocalDate.of(1945, 1, 10))
                                        .location(elderly1LocationJson)
                                        .gender(EnumGenderType.FEMALE)
                                        .avatarUrl("https://example.com/avatar/elderly1.jpg")
                                        .profileData("{\"bloodType\":\"A+\",\"allergies\":[\"Penicillin\"]}")
                                        .careRequirement("{\"dailyMedication\":true,\"mobilityAid\":\"walker\"}")
                                        .note("Cần hỗ trợ đi lại và uống thuốc đúng giờ")
                                        .healthNote("Huyết áp cao, cần theo dõi thường xuyên")
                                        .status(EnumActivationStatusType.ACTIVE)
                                        .careSeekerProfile(careSeekerProfile)
                                        .build();
                        elderlyProfileRepository.save(elderlyProfile1);
                        log.info("ElderlyProfile 1 created: {}", elderlyProfile1.getFullName());

                        // Create ElderlyProfile 2
                        String elderly2LocationJson = createLocationJson("123 Đường Nguyễn Văn A, Quận 1, TP.HCM",
                                        10.762622, 106.660172);
                        ElderlyProfile elderlyProfile2 = ElderlyProfile.builder()
                                        .fullName("Nguyễn Văn Ông")
                                        .phoneNumber("0902222222")
                                        .birthDate(LocalDate.of(1942, 6, 25))
                                        .location(elderly2LocationJson)
                                        .gender(EnumGenderType.MALE)
                                        .avatarUrl("https://example.com/avatar/elderly2.jpg")
                                        .profileData("{\"bloodType\":\"O+\",\"allergies\":[]}")
                                        .careRequirement("{\"dailyMedication\":true,\"physicalTherapy\":true}")
                                        .note("Cần vật lý trị liệu hàng ngày")
                                        .healthNote("Đã phẫu thuật thay khớp gối, cần tập luyện nhẹ nhàng")
                                        .status(EnumActivationStatusType.ACTIVE)
                                        .careSeekerProfile(careSeekerProfile)
                                        .build();
                        elderlyProfileRepository.save(elderlyProfile2);
                        log.info("ElderlyProfile 2 created: {}", elderlyProfile2.getFullName());

                        log.info("Sample profiles initialized successfully");
                }
        }

        private void initQualificationTypes() {
                if (qualificationTypeRepository.count() == 0) {
                        log.info("Initializing qualification types...");

                        // Chứng chỉ điều dưỡng
                        QualificationType nursingCertificate = QualificationType.builder()
                                        .typeName("Chứng chỉ điều dưỡng")
                                        .description("Chứng chỉ chuyên môn về điều dưỡng và chăm sóc sức khỏe")
                                        .isActive(true)
                                        .build();
                        qualificationTypeRepository.save(nursingCertificate);
                        log.info("QualificationType created: {}", nursingCertificate.getTypeName());

                        // Bằng vật lý trị liệu
                        QualificationType physicalTherapyDegree = QualificationType.builder()
                                        .typeName("Bằng vật lý trị liệu")
                                        .description("Bằng cấp chuyên môn về vật lý trị liệu và phục hồi chức năng")
                                        .isActive(true)
                                        .build();
                        qualificationTypeRepository.save(physicalTherapyDegree);
                        log.info("QualificationType created: {}", physicalTherapyDegree.getTypeName());

                        // Chứng chỉ sơ cấp cứu
                        QualificationType firstAidCertificate = QualificationType.builder()
                                        .typeName("Chứng chỉ sơ cấp cứu")
                                        .description("Chứng chỉ về kỹ năng sơ cấp cứu và xử lý tình huống khẩn cấp")
                                        .isActive(true)
                                        .build();
                        qualificationTypeRepository.save(firstAidCertificate);
                        log.info("QualificationType created: {}", firstAidCertificate.getTypeName());

                        // Chứng chỉ chăm sóc người cao tuổi
                        QualificationType elderlyCareCertificate = QualificationType.builder()
                                        .typeName("Chứng chỉ chăm sóc người cao tuổi")
                                        .description("Chứng chỉ chuyên môn về chăm sóc và hỗ trợ người cao tuổi")
                                        .isActive(true)
                                        .build();
                        qualificationTypeRepository.save(elderlyCareCertificate);
                        log.info("QualificationType created: {}", elderlyCareCertificate.getTypeName());

                        // Bằng cấp y khoa
                        QualificationType medicalDegree = QualificationType.builder()
                                        .typeName("Bằng cấp y khoa")
                                        .description("Bằng cấp y khoa về y học và điều trị bệnh")
                                        .isActive(true)
                                        .build();
                        qualificationTypeRepository.save(medicalDegree);
                        log.info("QualificationType created: {}", medicalDegree.getTypeName());

                        // Chứng chỉ/giấy xác nhận tập huấn kiến thức an toàn thực phẩm
                        QualificationType foodSafetyCertificate = QualificationType.builder()
                                        .typeName("Giấy xác nhận tập huấn kiến thức an toàn thực phẩm")
                                        .description("Giấy xác nhận đã được tập huấn kiến thức an toàn thực phẩm cho chủ cơ sở và người trực tiếp nấu")
                                        .isActive(true)
                                        .build();
                        qualificationTypeRepository.save(foodSafetyCertificate);
                        log.info("QualificationType created: {}", foodSafetyCertificate.getTypeName());

                        log.info("Qualification types initialized successfully");
                }
        }

        private void initServicePackages() {
                if (servicePackageRepository.count() == 0) {
                        log.info("Initializing service packages...");

                        // Lấy các qualification types để tạo requirements
                        QualificationType physicalTherapyType = qualificationTypeRepository
                                        .findByTypeNameAndDeletedIsFalse("Bằng vật lý trị liệu");
                        QualificationType nursingType = qualificationTypeRepository
                                        .findByTypeNameAndDeletedIsFalse("Chứng chỉ điều dưỡng");
                        QualificationType foodSafetyType = qualificationTypeRepository
                                        .findByTypeNameAndDeletedIsFalse(
                                                        "Giấy xác nhận tập huấn kiến thức an toàn thực phẩm");

                        // Gói Cơ Bản (Basic Care) - BR-PACKAGE-001
                        // Requirements: ≥3 skills, Rating ≥4.0/5.0
                        QualificationRequirements basicQualification = QualificationRequirements.builder()
                                        .skills(java.util.Arrays.asList(
                                                        "Kỹ năng chăm sóc cá nhân",
                                                        "Kỹ năng giao tiếp",
                                                        "Kỹ năng hỗ trợ ăn uống",
                                                        "Kỹ năng massage cơ bản"))
                                        .certificateGroups(null) // Không yêu cầu chứng chỉ
                                        .build();
                        String basicQualificationJson = convertQualificationToJson(basicQualification);

                        ServicePackage basicPackage = ServicePackage.builder()
                                        .packageName("Gói Cơ Bản")
                                        .description("Gói chăm sóc cơ bản cho người cao tuổi, bao gồm các dịch vụ thiết yếu hàng ngày. Yêu cầu: ≥3 skills, Rating ≥4.0/5.0")
                                        .durationHours(4)
                                        .packageType(EnumServicePackageType.BASIC)
                                        .price(400000.0)
                                        .note("Phù hợp cho người cao tuổi cần hỗ trợ sinh hoạt hàng ngày. Phải đặt trước tối thiểu 12 giờ. Working hours: 7:00 AM - 5:00 PM")
                                        .qualification(basicQualificationJson)
                                        .status(EnumActivationStatusType.ACTIVE)
                                        .build();
                        basicPackage = servicePackageRepository.save(basicPackage);
                        log.info("ServicePackage created: {}", basicPackage.getPackageName());

                        // Tasks cho Gói Cơ Bản
                        createServiceTask(basicPackage, "Tắm rửa vệ sinh cá nhân",
                                        "Hỗ trợ tắm rửa, vệ sinh cá nhân cho người cao tuổi");
                        createServiceTask(basicPackage, "Cho ăn uống",
                                        "Hỗ trợ chuẩn bị và cho ăn uống đúng giờ");
                        createServiceTask(basicPackage, "Massage cơ bản",
                                        "Massage nhẹ nhàng để thư giãn và lưu thông máu");
                        createServiceTask(basicPackage, "Trò chuyện, đọc báo, sinh hoạt tinh thần",
                                        "Trò chuyện, đọc báo, tham gia các hoạt động tinh thần");

                        // Gói Chuyên Nghiệp (Professional Care) - BR-PACKAGE-002
                        // Requirements: Chứng chỉ vật lý trị liệu HOẶC điều dưỡng, Rating ≥4.3/5.0
                        java.util.List<java.util.UUID> professionalCertGroup = new java.util.ArrayList<>();
                        if (physicalTherapyType != null) {
                                professionalCertGroup.add(physicalTherapyType.getQualificationTypeId());
                        }
                        if (nursingType != null) {
                                professionalCertGroup.add(nursingType.getQualificationTypeId());
                        }
                        QualificationRequirements professionalQualification = QualificationRequirements.builder()
                                        .skills(null) // Không yêu cầu skills cụ thể
                                        .certificateGroups(java.util.Arrays.asList(professionalCertGroup)) // Vật lý trị
                                                                                                           // liệu HOẶC
                                                                                                           // điều dưỡng
                                        .build();
                        String professionalQualificationJson = convertQualificationToJson(professionalQualification);

                        ServicePackage professionalPackage = ServicePackage.builder()
                                        .packageName("Gói Chuyên Nghiệp")
                                        .description("Gói chăm sóc chuyên nghiệp với các dịch vụ phục hồi chức năng và theo dõi sức khỏe. Yêu cầu: Chứng chỉ vật lý trị liệu HOẶC điều dưỡng, Rating ≥4.3/5.0")
                                        .durationHours(8)
                                        .packageType(EnumServicePackageType.PROFESSIONAL)
                                        .price(750000.0)
                                        .note("Phù hợp cho người cao tuổi cần phục hồi chức năng và theo dõi sức khỏe. Phải đặt trước tối thiểu 24 giờ. Working hours: 7:00 AM - 5:00 PM")
                                        .qualification(professionalQualificationJson)
                                        .status(EnumActivationStatusType.ACTIVE)
                                        .build();
                        professionalPackage = servicePackageRepository.save(professionalPackage);
                        log.info("ServicePackage created: {}", professionalPackage.getPackageName());

                        // Tasks cho Gói Chuyên Nghiệp
                        createServiceTask(professionalPackage, "Tập vật lý trị liệu",
                                        "Các bài tập vật lý trị liệu phù hợp với tình trạng sức khỏe");
                        createServiceTask(professionalPackage, "Massage phục hồi chức năng",
                                        "Massage chuyên sâu để phục hồi chức năng vận động");
                        createServiceTask(professionalPackage, "Theo dõi tiến trình sức khỏe",
                                        "Theo dõi và ghi chép tiến trình sức khỏe hàng ngày");

                        // Gói Nâng Cao (Premium Care) - BR-PACKAGE-003
                        // Requirements: ≥2 chứng chỉ (1 trong nhóm vật lý trị liệu/điều dưỡng + 1 về
                        // dinh dưỡng), Rating ≥4.5/5.0
                        java.util.List<java.util.UUID> professionalCertGroupForAdvanced = new java.util.ArrayList<>();
                        if (physicalTherapyType != null) {
                                professionalCertGroupForAdvanced.add(physicalTherapyType.getQualificationTypeId());
                        }
                        if (nursingType != null) {
                                professionalCertGroupForAdvanced.add(nursingType.getQualificationTypeId());
                        }
                        java.util.List<java.util.UUID> foodSafetyCertGroup = new java.util.ArrayList<>();
                        if (foodSafetyType != null) {
                                foodSafetyCertGroup.add(foodSafetyType.getQualificationTypeId());
                        }
                        QualificationRequirements advancedQualification = QualificationRequirements.builder()
                                        .skills(null) // Không yêu cầu skills cụ thể
                                        .certificateGroups(java.util.Arrays.asList(
                                                        professionalCertGroupForAdvanced, // 1 trong nhóm vật lý trị
                                                                                          // liệu/điều dưỡng
                                                        foodSafetyCertGroup)) // VÀ 1 về dinh dưỡng/an toàn thực phẩm
                                        .build();
                        String advancedQualificationJson = convertQualificationToJson(advancedQualification);

                        ServicePackage advancedPackage = ServicePackage.builder()
                                        .packageName("Gói Nâng Cao")
                                        .description("Gói chăm sóc toàn diện bao gồm tất cả dịch vụ cơ bản, chuyên nghiệp và thêm dịch vụ nấu ăn, dọn dẹp. Yêu cầu: ≥2 chứng chỉ (1 trong nhóm vật lý trị liệu/điều dưỡng + 1 về dinh dưỡng), Rating ≥4.5/5.0")
                                        .durationHours(8)
                                        .packageType(EnumServicePackageType.ADVANCED)
                                        .price(1100000.0)
                                        .note("Gói chăm sóc toàn diện nhất, phù hợp cho người cao tuổi cần hỗ trợ toàn bộ. Phải đặt trước tối thiểu 48 giờ. Working hours: 7:00 AM - 5:00 PM")
                                        .qualification(advancedQualificationJson)
                                        .status(EnumActivationStatusType.ACTIVE)
                                        .build();
                        advancedPackage = servicePackageRepository.save(advancedPackage);
                        log.info("ServicePackage created: {}", advancedPackage.getPackageName());

                        // Tasks cho Gói Nâng Cao (bao gồm tất cả tasks của 2 gói trên + 2 tasks mới)
                        // Tasks từ Gói Cơ Bản
                        createServiceTask(advancedPackage, "Tắm rửa vệ sinh cá nhân",
                                        "Hỗ trợ tắm rửa, vệ sinh cá nhân cho người cao tuổi");
                        createServiceTask(advancedPackage, "Cho ăn uống",
                                        "Hỗ trợ chuẩn bị và cho ăn uống đúng giờ");
                        createServiceTask(advancedPackage, "Massage cơ bản",
                                        "Massage nhẹ nhàng để thư giãn và lưu thông máu");
                        createServiceTask(advancedPackage, "Trò chuyện, đọc báo, sinh hoạt tinh thần",
                                        "Trò chuyện, đọc báo, tham gia các hoạt động tinh thần");

                        // Tasks từ Gói Chuyên Nghiệp
                        createServiceTask(advancedPackage, "Tập vật lý trị liệu",
                                        "Các bài tập vật lý trị liệu phù hợp với tình trạng sức khỏe");
                        createServiceTask(advancedPackage, "Massage phục hồi chức năng",
                                        "Massage chuyên sâu để phục hồi chức năng vận động");
                        createServiceTask(advancedPackage, "Theo dõi tiến trình sức khỏe",
                                        "Theo dõi và ghi chép tiến trình sức khỏe hàng ngày");

                        // Tasks mới cho Gói Nâng Cao
                        createServiceTask(advancedPackage, "Nấu ăn theo chế độ",
                                        "Nấu các món ăn phù hợp với chế độ dinh dưỡng và sức khỏe");
                        createServiceTask(advancedPackage, "Dọn dẹp vệ sinh nhà cửa",
                                        "Dọn dẹp và vệ sinh không gian sống");

                        log.info("Service packages initialized successfully");
                }
        }

        /**
         * Convert QualificationRequirements to JSON string
         */
        private String convertQualificationToJson(QualificationRequirements qualification) {
                if (qualification == null) {
                        return null;
                }
                try {
                        return objectMapper.writeValueAsString(qualification);
                } catch (Exception e) {
                        log.error("Error converting qualification to JSON", e);
                        return null;
                }
        }

        private void createServiceTask(ServicePackage servicePackage, String taskName, String description) {
                ServiceTask task = ServiceTask.builder()
                                .taskName(taskName)
                                .description(description)
                                .status(EnumActivationStatusType.ACTIVE)
                                .servicePackage(servicePackage)
                                .build();
                serviceTaskRepository.save(task);
        }

        private String createLocationJson(String address, Double latitude, Double longitude) {
                try {
                        LocationRequest location = new LocationRequest(address, latitude, longitude);
                        return objectMapper.writeValueAsString(location);
                } catch (Exception e) {
                        log.error("Failed to create location JSON", e);
                        return "{\"address\":\"" + address + "\",\"latitude\":" + latitude + ",\"longitude\":"
                                        + longitude + "}";
                }
        }
}
