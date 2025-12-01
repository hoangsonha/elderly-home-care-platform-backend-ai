package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.configurations.CustomAccountDetail;
import com.capstone_project.elderly_platform.configurations.JWTAuthenticationFilter;
import com.capstone_project.elderly_platform.configurations.JwtTokenConfiguration;
import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.pojos.CareSeekerProfile;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.pojos.Role;
import com.capstone_project.elderly_platform.dtos.request.AccountRegisterRequest;
import com.capstone_project.elderly_platform.dtos.request.AccountVerificationRequest;
import com.capstone_project.elderly_platform.dtos.request.ResendCodeVerifyRequest;
import com.capstone_project.elderly_platform.dtos.response.TokenResponse;
import com.capstone_project.elderly_platform.enums.EnumRoleType;
import com.capstone_project.elderly_platform.enums.EnumTokenType;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementExistException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.exceptions.EntityNotFoundException;
import com.capstone_project.elderly_platform.mappers.CareSeekerProfileMapper;
import com.capstone_project.elderly_platform.mappers.CaregiverProfileMapper;
import com.capstone_project.elderly_platform.repositories.AccountRepository;
import com.capstone_project.elderly_platform.repositories.CareSeekerProfileRepository;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final RoleService roleService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtTokenConfiguration jwtTokenConfiguration;
    private final JWTAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationManager authenticationManager;

    private final JavaMailSender javaMailSender;

    private final SpringTemplateEngine templateEngine;

    private final CareSeekerProfileRepository careSeekerProfileRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final CareSeekerProfileMapper careSeekerProfileMapper;
    private final CaregiverProfileMapper caregiverProfileMapper;

    @Value("${spring.mail.username}")
    private String from;

    @Transactional
    @Override
    public boolean registerAccount(AccountRegisterRequest accountRegisterRequest) {
        Account checkExistingAccount = accountRepository.getAccountByEmail(accountRegisterRequest.getEmail());
        if (checkExistingAccount != null) {
            throw new ElementExistException("Tài khoản đã tồn tại");
        }

        Role role = null;

        if (accountRegisterRequest.getRole() != null && accountRegisterRequest.getRole().equals("ROLE_CARE_SEEKER")) {
            role = roleService.getRoleByRoleName(EnumRoleType.ROLE_CARE_SEEKER);
        } else if (accountRegisterRequest.getRole() != null
                && accountRegisterRequest.getRole().equals("ROLE_CAREGIVER")) {
            role = roleService.getRoleByRoleName(EnumRoleType.ROLE_CAREGIVER);
        } else {
            throw new BadRequestException("Vai trò không hợp lệ");
        }

        String codeVerify = generateSixDigitCode();
        LocalDateTime codeVerifyExpiresAt = LocalDateTime.now().plusMinutes(10);

        Account account = Account.builder()
                .email(accountRegisterRequest.getEmail())
                .password(bCryptPasswordEncoder.encode(accountRegisterRequest.getPassword()))
                .accessToken(null)
                .refreshToken(null)
                .enabled(false)
                .nonLocked(false)
                .role(role)
                .codeVerify(codeVerify)
                .codeVerifyExpiresAt(codeVerifyExpiresAt)
                .build();

        Account accountSave = accountRepository.save(account);

        return sendVerificationEmail(accountSave.getEmail(), accountSave.getRole().getRoleName().name(),
                accountSave.getCodeVerify());
    }

    @Override
    public TokenResponse verificationUser(AccountVerificationRequest request) {
        Account account = accountRepository.getAccountByEmail(request.getEmail());

        if (account == null) {
            throw new BadRequestException("Tài khoản không tồn tại");
        }

        // Kiểm tra mã verify có tồn tại không
        if (account.getCodeVerify() == null) {
            throw new BadRequestException("Mã xác thực không tồn tại. Vui lòng yêu cầu mã mới");
        }

        // Kiểm tra mã verify đã hết hạn chưa
        if (account.getCodeVerifyExpiresAt() == null ||
                LocalDateTime.now().isAfter(account.getCodeVerifyExpiresAt())) {
            throw new BadRequestException("Mã xác thực đã hết hạn. Vui lòng yêu cầu mã mới");
        }

        if (request.getVerificationCode().equals(account.getCodeVerify())) {
            account.setCodeVerify(null);
            account.setCodeVerifyExpiresAt(null);
            account.setEnabled(true);
            account.setNonLocked(true);

            CustomAccountDetail accountDetail = CustomAccountDetail.mapAccountToAccountDetail(account);
            String token = jwtTokenConfiguration.generatedToken(accountDetail);
            String refreshToken = jwtTokenConfiguration.generatedRefreshToken(accountDetail);

            account.setRefreshToken(refreshToken);
            account.setAccessToken(token);
            accountRepository.save(account);

            // Lấy thông tin profile dựa vào role
            Object profileData = null;
            boolean hasProfile = false;
            String roleName = account.getRole() != null ? account.getRole().getRoleName().name() : null;

            if (account.getRole() != null) {
                EnumRoleType roleType = account.getRole().getRoleName();

                if (roleType == EnumRoleType.ROLE_CARE_SEEKER) {
                    CareSeekerProfile careSeekerProfile = careSeekerProfileRepository
                            .findByAccount_AccountIdAndDeletedIsFalse(account.getAccountId());
                    if (careSeekerProfile != null) {
                        profileData = careSeekerProfileMapper.toDTO(careSeekerProfile);
                        hasProfile = true;
                    }
                } else if (roleType == EnumRoleType.ROLE_CAREGIVER) {
                    CaregiverProfile caregiverProfile = caregiverProfileRepository
                            .findByAccount_AccountIdAndDeletedIsFalse(account.getAccountId());
                    if (caregiverProfile != null) {
                        profileData = caregiverProfileMapper.toDTO(caregiverProfile);
                        hasProfile = true;
                    }
                }
            }

            return TokenResponse.builder()
                    .code("Success")
                    .message("Xác thực thành công")
                    .accountId(account.getAccountId())
                    .email(account.getEmail())
                    .token(token)
                    .refreshToken(refreshToken)
                    .roleName(roleName)
                    .avatarUrl(account.getAvatarUrl())
                    .enabled(account.getEnabled())
                    .nonLocked(account.getNonLocked())
                    .hasProfile(hasProfile)
                    .profile(profileData)
                    .build();
        }
        throw new BadRequestException("Mã xác thực không đúng. Vui lòng thử lại");
    }

    @Override
    @Transactional
    public boolean resendCodeVerify(ResendCodeVerifyRequest request) {
        Account account = accountRepository.getAccountByEmail(request.getEmail());

        if (account == null) {
            throw new BadRequestException("Tài khoản không tồn tại");
        }

        // Kiểm tra account đã được verify chưa
        if (account.getEnabled()) {
            throw new BadRequestException("Tài khoản đã được xác thực. Không cần gửi lại mã");
        }

        // Tạo mã verify mới và set expiration time (10 phút)
        String newCodeVerify = generateSixDigitCode();
        LocalDateTime codeVerifyExpiresAt = LocalDateTime.now().plusMinutes(10);

        account.setCodeVerify(newCodeVerify);
        account.setCodeVerifyExpiresAt(codeVerifyExpiresAt);
        accountRepository.save(account);

        // Gửi email với mã verify mới
        String roleName = account.getRole() != null ? account.getRole().getRoleName().name() : null;
        return sendVerificationEmail(account.getEmail(), roleName, newCodeVerify);
    }

    public String generateSixDigitCode() {
        Random random = new Random();
        int number = random.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    private boolean sendVerificationEmail(String email, String role, String verificationCode) {
        // String recipient, String subject, String content, MultipartFile[] files
        if (email == null) {
            return false;
        }
        try {
            Context context = new Context();
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("name",
                    role.equals(EnumRoleType.ROLE_CARE_SEEKER.name()) ? "Người thuê mới" : "Người chăm sóc mới");
            context.setVariable("role", role.equals(EnumRoleType.ROLE_CARE_SEEKER.name()) ? "seeker" : "caregiver");

            String content = "confirm";

            String mailne = templateEngine.process(content, context);

            String title = "Mã xác nhận tài khoản";
            String senderName = "ELDERLY PLATFORM";
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from, senderName);

            if (email.contains(",")) {
                helper.setTo(InternetAddress.parse(email));
            } else {
                helper.setTo(email);
            }
            helper.setSubject(title);
            helper.setText(mailne, true);
            javaMailSender.send(mimeMessage);
            log.error("Send mail to {}", email);
            return true;
        } catch (Exception e) {
            log.error("Can not send mail {}", e.toString());
            return false;
        }
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        TokenResponse tokenResponse = TokenResponse.builder()
                .code("Failed")
                .message("Làm mới token thất bại")
                .build();
        String email = jwtTokenConfiguration.getEmailFromJwt(refreshToken, EnumTokenType.REFRESH_TOKEN);
        Account account = accountRepository.getAccountByEmail(email);
        if (account != null) {
            if (StringUtils.hasText(refreshToken) && account.getRefreshToken().equals(refreshToken)) {
                if (jwtTokenConfiguration.validate(refreshToken, EnumTokenType.REFRESH_TOKEN)) {
                    CustomAccountDetail customAccountDetail = CustomAccountDetail.mapAccountToAccountDetail(account);
                    if (customAccountDetail != null) {
                        String newToken = jwtTokenConfiguration.generatedToken(customAccountDetail);
                        account.setAccessToken(newToken);
                        accountRepository.save(account);

                        // Lấy thông tin profile dựa vào role
                        Object profileData = null;
                        boolean hasProfile = false;
                        String roleName = account.getRole() != null ? account.getRole().getRoleName().name() : null;

                        if (account.getRole() != null) {
                            EnumRoleType roleType = account.getRole().getRoleName();

                            if (roleType == EnumRoleType.ROLE_CARE_SEEKER) {
                                CareSeekerProfile careSeekerProfile = careSeekerProfileRepository
                                        .findByAccount_AccountIdAndDeletedIsFalse(account.getAccountId());
                                if (careSeekerProfile != null) {
                                    profileData = careSeekerProfileMapper.toDTO(careSeekerProfile);
                                    hasProfile = true;
                                }
                            } else if (roleType == EnumRoleType.ROLE_CAREGIVER) {
                                CaregiverProfile caregiverProfile = caregiverProfileRepository
                                        .findByAccount_AccountIdAndDeletedIsFalse(account.getAccountId());
                                if (caregiverProfile != null) {
                                    profileData = caregiverProfileMapper.toDTO(caregiverProfile);
                                    hasProfile = true;
                                }
                            }
                        }

                        tokenResponse = TokenResponse.builder()
                                .code("Success")
                                .message("Làm mới token thành công")
                                .accountId(account.getAccountId())
                                .token(newToken)
                                .refreshToken(refreshToken)
                                .email(account.getEmail())
                                .roleName(roleName)
                                .avatarUrl(account.getAvatarUrl())
                                .enabled(account.getEnabled())
                                .nonLocked(account.getNonLocked())
                                .hasProfile(hasProfile)
                                .profile(profileData)
                                .build();
                    }
                }
            }
        }
        return tokenResponse;
    }

    @Override
    public TokenResponse login(String email, String password) {
        TokenResponse tokenResponse = TokenResponse.builder()
                .code("Failed")
                .message("Login failed")
                .build();
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                email, password);
        Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        CustomAccountDetail accountDetail = (CustomAccountDetail) authentication.getPrincipal();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String token = jwtTokenConfiguration.generatedToken(accountDetail);
        String refreshToken = jwtTokenConfiguration.generatedRefreshToken(accountDetail);
        Account account = accountRepository.getAccountByEmail(accountDetail.getEmail());
        if (account != null) {
            account.setRefreshToken(refreshToken);
            account.setAccessToken(token);
            accountRepository.save(account);

            // Lấy thông tin profile dựa vào role
            Object profileData = null;
            boolean hasProfile = false;
            String roleName = account.getRole() != null ? account.getRole().getRoleName().name() : null;

            if (account.getRole() != null) {
                EnumRoleType roleType = account.getRole().getRoleName();

                if (roleType == EnumRoleType.ROLE_CARE_SEEKER) {
                    // Lấy CareSeekerProfile
                    CareSeekerProfile careSeekerProfile = careSeekerProfileRepository
                            .findByAccount_AccountIdAndDeletedIsFalse(account.getAccountId());
                    if (careSeekerProfile != null) {
                        profileData = careSeekerProfileMapper.toDTO(careSeekerProfile);
                        hasProfile = true;
                    }
                } else if (roleType == EnumRoleType.ROLE_CAREGIVER) {
                    // Lấy CaregiverProfile
                    CaregiverProfile caregiverProfile = caregiverProfileRepository
                            .findByAccount_AccountIdAndDeletedIsFalse(account.getAccountId());
                    if (caregiverProfile != null) {
                        profileData = caregiverProfileMapper.toDTO(caregiverProfile);
                        hasProfile = true;
                    }
                }
                // ROLE_ADMIN không có profile nên profileData sẽ là null và hasProfile = false
            }

            tokenResponse = TokenResponse.builder()
                    .code("Success")
                    .message("Login successfully")
                    .accountId(account.getAccountId())
                    .email(account.getEmail())
                    .token(token)
                    .refreshToken(refreshToken)
                    .roleName(roleName)
                    .avatarUrl(account.getAvatarUrl())
                    .enabled(account.getEnabled())
                    .nonLocked(account.getNonLocked())
                    .hasProfile(hasProfile)
                    .profile(profileData)
                    .build();
        }
        return tokenResponse;
    }

    @Override
    public boolean logout(HttpServletRequest request) {
        String token = jwtAuthenticationFilter.getToken(request);
        String email = jwtTokenConfiguration.getEmailFromJwt(token, EnumTokenType.TOKEN);
        Account account = accountRepository.getAccountByEmail(email);
        if (account == null) {
            throw new ElementNotFoundException("Không tìm thấy tài khoản");
        }
        account.setAccessToken(null);
        account.setRefreshToken(null);
        Account checkUser = accountRepository.save(account);

        return checkUser.getAccessToken() == null;
    }

    @Override
    public Account getAccountById(UUID id) {
        return accountRepository.findByAccountIdAndDeletedIsFalse(id).orElseThrow(
                () -> new EntityNotFoundException("Không tìm thấy người dùng"));
    }

    // @Override
    // public PagingResponse findAll(int currentPage, int pageSize) {
    // Pageable pageable = PageRequest.of(currentPage - 1, pageSize);
    //
    // var pageData = employeeRepository.findAll(pageable);
    //
    // return PagingResponse.builder()
    // .currentPage(currentPage)
    // .pageSize(pageSize)
    // .totalElements(pageData.getTotalElements())
    // .totalPages(pageData.getTotalPages())
    // .data(pageData.getContent())
    // .build();
    // }
    //
    // @Override
    // public Employee findById(Integer id) {
    // return this.employeeRepository.findById(id).orElse(null);
    // }
    //
    // @Override
    // public Employee save(Employee entity) {
    // return this.employeeRepository.save(entity);
    // }
}
