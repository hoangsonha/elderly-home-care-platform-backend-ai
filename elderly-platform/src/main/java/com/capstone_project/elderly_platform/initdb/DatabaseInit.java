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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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
        private final ObjectMapper objectMapper;

        @Override
    public void run(String... args) {
                initRoles();
                Account adminAccount = initAccounts();
                initSystemConfigs(adminAccount != null ? adminAccount.getAccountId() : null);
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

                        Role adminRole = roleRepository.getRoleByRoleName(EnumRoleType.ROLE_ADMIN).get(0);
                        Role seekerRole = roleRepository.getRoleByRoleName(EnumRoleType.ROLE_CARE_SEEKER).get(0);
                        Role caregiverRole = roleRepository.getRoleByRoleName(EnumRoleType.ROLE_CAREGIVER).get(0);

                        // Admin account
                        Account adminAccount = Account.builder()
                                        .email("admin@elderlycare.com")
                                        .password(bCryptPasswordEncoder.encode("Admin@123"))
                                        .enabled(true)
                                        .nonLocked(true)
                                        .role(adminRole)
                                        .build();
                        adminAccount = accountRepository.save(adminAccount);
                        log.info("Admin account created: admin@elderlycare.com / Admin@123");

                        // Care Seeker account
                        Account seekerAccount = Account.builder()
                                        .email("seeker@elderlycare.com")
                                        .password(bCryptPasswordEncoder.encode("Seeker@123"))
                                        .enabled(true)
                                        .nonLocked(true)
                                        .role(seekerRole)
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
                                        .role(caregiverRole)
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
                                        .role(caregiverRole)
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
                                                        account.getRole().getRoleName() == EnumRoleType.ROLE_CARE_SEEKER)
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
                        String seekerLocationJson = createLocationJson("123 Đường Nguyễn Văn A, Quận 1, TP.HCM", 10.762622, 106.660172);
                        CareSeekerProfile careSeekerProfile = CareSeekerProfile.builder()
                                        .fullName("Nguyễn Văn Tìm")
                                        .phoneNumber("0901234567")
                                        .location(seekerLocationJson)
                                        .birthDate(LocalDate.of(1985, 5, 15))
                                        .gender(EnumGenderType.MALE)
                                        .profileData("{\"preferredContactTime\":\"9:00-17:00\"}")
                                        .account(seekerAccount)
                                        .build();
                        careSeekerProfile = careSeekerProfileRepository.save(careSeekerProfile);
                        log.info("CareSeekerProfile created: {}", careSeekerProfile.getFullName());

                        // Create CaregiverProfile 1
                        String caregiver1LocationJson = createLocationJson("456 Đường Lê Lợi, Quận 3, TP.HCM", 10.7769, 106.7009);
                        CaregiverProfile caregiverProfile1 = CaregiverProfile.builder()
                                        .fullName("Trần Thị Chăm Sóc")
                                        .phoneNumber("0902345678")
                                        .location(caregiver1LocationJson)
                                        .bio("Kinh nghiệm 5 năm chăm sóc người cao tuổi, chuyên về bệnh Alzheimer và Parkinson")
                                        .isVerified(true)
                                        .birthDate(LocalDate.of(1990, 8, 20))
                                        .gender(EnumGenderType.FEMALE)
                                        .profileData("{\"experience\":\"5 years\",\"specializations\":[\"Alzheimer\",\"Parkinson\"]}")
                                        .account(caregiverAccount1)
                                        .build();
                        caregiverProfileRepository.save(caregiverProfile1);
                        log.info("CaregiverProfile 1 created: {}", caregiverProfile1.getFullName());

                        // Create CaregiverProfile 2
                        String caregiver2LocationJson = createLocationJson("789 Đường Võ Văn Tần, Quận 10, TP.HCM", 10.7730, 106.6660);
                        CaregiverProfile caregiverProfile2 = CaregiverProfile.builder()
                                        .fullName("Lê Văn Yêu Thương")
                                        .phoneNumber("0903456789")
                                        .location(caregiver2LocationJson)
                                        .bio("Chuyên viên vật lý trị liệu, có chứng chỉ chăm sóc người cao tuổi quốc tế")
                                        .isVerified(true)
                                        .birthDate(LocalDate.of(1988, 3, 10))
                                        .gender(EnumGenderType.MALE)
                                        .profileData("{\"experience\":\"7 years\",\"certifications\":[\"International Elderly Care\"]}")
                                        .account(caregiverAccount2)
                                        .build();
                        caregiverProfileRepository.save(caregiverProfile2);
                        log.info("CaregiverProfile 2 created: {}", caregiverProfile2.getFullName());

                        // Create ElderlyProfile 1
                        String elderly1LocationJson = createLocationJson("123 Đường Nguyễn Văn A, Quận 1, TP.HCM", 10.762622, 106.660172);
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
                        String elderly2LocationJson = createLocationJson("123 Đường Nguyễn Văn A, Quận 1, TP.HCM", 10.762622, 106.660172);
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

        private void initServicePackages() {
                if (servicePackageRepository.count() == 0) {
                        log.info("Initializing service packages...");

                        // Gói Cơ Bản (Basic Care)
                        String basicServiceIncluded = "[\"Tắm rửa vệ sinh cá nhân\",\"Cho ăn uống\",\"Massage cơ bản\",\"Trò chuyện, đọc báo, sinh hoạt tinh thần\"]";
                        ServicePackage basicPackage = ServicePackage.builder()
                                        .packageName("Gói Cơ Bản")
                                        .description("Gói chăm sóc cơ bản cho người cao tuổi, bao gồm các dịch vụ thiết yếu hàng ngày")
                                        .durationHours(4)
                                        .packageType(EnumServicePackageType.BASIC)
                                        .price(400000.0)
                                        .note("Phù hợp cho người cao tuổi cần hỗ trợ sinh hoạt hàng ngày")
                                        .serviceIncluded(basicServiceIncluded)
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

                        // Gói Chuyên Nghiệp (Professional Care)
                        String professionalServiceIncluded = "[\"Tập vật lý trị liệu\",\"Massage phục hồi chức năng\",\"Theo dõi tiến trình sức khỏe\"]";
                        ServicePackage professionalPackage = ServicePackage.builder()
                                        .packageName("Gói Chuyên Nghiệp")
                                        .description("Gói chăm sóc chuyên nghiệp với các dịch vụ phục hồi chức năng và theo dõi sức khỏe")
                                        .durationHours(8)
                                        .packageType(EnumServicePackageType.PROFESSIONAL)
                                        .price(750000.0)
                                        .note("Phù hợp cho người cao tuổi cần phục hồi chức năng và theo dõi sức khỏe")
                                        .serviceIncluded(professionalServiceIncluded)
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

                        // Gói Nâng Cao (Premium Care)
                        String advancedServiceIncluded = "[\"Tất cả tasks của Gói Cơ Bản\",\"Tất cả tasks của Gói Chuyên Nghiệp\",\"Nấu ăn theo chế độ\",\"Dọn dẹp vệ sinh nhà cửa\"]";
                        ServicePackage advancedPackage = ServicePackage.builder()
                                        .packageName("Gói Nâng Cao")
                                        .description("Gói chăm sóc toàn diện bao gồm tất cả dịch vụ cơ bản, chuyên nghiệp và thêm dịch vụ nấu ăn, dọn dẹp")
                                        .durationHours(8)
                                        .packageType(EnumServicePackageType.ADVANCED)
                                        .price(1100000.0)
                                        .note("Gói chăm sóc toàn diện nhất, phù hợp cho người cao tuổi cần hỗ trợ toàn bộ")
                                        .serviceIncluded(advancedServiceIncluded)
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
                        return "{\"address\":\"" + address + "\",\"latitude\":" + latitude + ",\"longitude\":" + longitude + "}";
                }
        }
}
