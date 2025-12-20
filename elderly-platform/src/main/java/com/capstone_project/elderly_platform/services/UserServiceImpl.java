package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.UpdateUserRequest;
import com.capstone_project.elderly_platform.dtos.response.PagingResponse;
import com.capstone_project.elderly_platform.dtos.response.UserResponse;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.repositories.AccountRepository;
import com.capstone_project.elderly_platform.utils.AccountSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public UserResponse lockUser(UUID accountId) {
        log.info("Locking user with account ID: {}", accountId);

        Account account = accountRepository.findByAccountIdAndDeletedIsFalse(accountId)
                .orElseThrow(() -> new ElementNotFoundException("User not found"));

        account.setEnabled(false);
        account.setNonLocked(false);

        Account savedAccount = accountRepository.save(account);
        log.info("User locked successfully: {}", accountId);

        return mapToUserResponse(savedAccount);
    }

    @Override
    @Transactional
    public UserResponse unlockUser(UUID accountId) {
        log.info("Unlocking user with account ID: {}", accountId);

        Account account = accountRepository.findByAccountIdAndDeletedIsFalse(accountId)
                .orElseThrow(() -> new ElementNotFoundException("User not found"));

        account.setEnabled(true);
        account.setNonLocked(true);

        Account savedAccount = accountRepository.save(account);
        log.info("User unlocked successfully: {}", accountId);

        return mapToUserResponse(savedAccount);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID accountId, UpdateUserRequest request) {
        log.info("Updating user with account ID: {}", accountId);

        Account account = accountRepository.findByAccountIdAndDeletedIsFalse(accountId)
                .orElseThrow(() -> new ElementNotFoundException("User not found"));

        // Check if email is being changed and if it already exists
        if (request.getEmail() != null && !request.getEmail().equals(account.getEmail())) {
            Account existingAccount = accountRepository.getAccountByEmail(request.getEmail());
            if (existingAccount != null && !existingAccount.getAccountId().equals(accountId)) {
                throw new BadRequestException("Email already exists");
            }
            account.setEmail(request.getEmail());
        }

        if (request.getAvatarUrl() != null) {
            account.setAvatarUrl(request.getAvatarUrl());
        }

        Account savedAccount = accountRepository.save(account);
        log.info("User updated successfully: {}", accountId);

        return mapToUserResponse(savedAccount);
    }

    @Override
    public PagingResponse getAllUsers(
            int currentPage,
            int pageSize,
            String email,
            Boolean isLocked,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        log.info(
                "Getting all users with filters - page: {}, size: {}, email: {}, isLocked: {}, startDate: {}, endDate: {}",
                currentPage, pageSize, email, isLocked, startDate, endDate);

        // Build specification
        Specification<Account> spec = AccountSpecification.notDeleted();

        if (email != null && !email.trim().isEmpty()) {
            spec = spec.and(AccountSpecification.searchByEmail(email));
        }

        if (isLocked != null) {
            spec = spec.and(AccountSpecification.filterByLockedStatus(isLocked));
        }

        if (startDate != null || endDate != null) {
            spec = spec.and(AccountSpecification.filterByDateRange(startDate, endDate));
        }

        // Create pageable with sorting by createdAt descending
        Pageable pageable = PageRequest.of(currentPage - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Execute query
        Page<Account> accountPage = accountRepository.findAll(spec, pageable);

        // Map to DTOs
        List<UserResponse> userResponses = accountPage.getContent().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());

        log.info("Found {} users out of {} total", userResponses.size(), accountPage.getTotalElements());

        return PagingResponse.builder()
                .code("Success")
                .message("Users retrieved successfully")
                .currentPage(currentPage)
                .totalPages(accountPage.getTotalPages())
                .elementPerPage(pageSize)
                .totalElements(accountPage.getTotalElements())
                .data(userResponses)
                .build();
    }

    private UserResponse mapToUserResponse(Account account) {
        // Get fullName from profile
        String fullName = null;
        if (account.getCareSeekerProfile() != null && !account.getCareSeekerProfile().isDeleted()) {
            fullName = account.getCareSeekerProfile().getFullName();
        } else if (account.getCaregiverProfile() != null && !account.getCaregiverProfile().isDeleted()) {
            fullName = account.getCaregiverProfile().getFullName();
        }

        String roleName = account.getRole() != null ? account.getRole().getRoleName().name() : null;

        return UserResponse.builder()
                .accountId(account.getAccountId())
                .email(account.getEmail())
                .enabled(account.getEnabled())
                .nonLocked(account.getNonLocked())
                .avatarUrl(account.getAvatarUrl())
                .roleName(roleName)
                .fullName(fullName)
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
