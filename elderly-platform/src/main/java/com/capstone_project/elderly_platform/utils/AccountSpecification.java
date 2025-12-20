package com.capstone_project.elderly_platform.utils;

import com.capstone_project.elderly_platform.enums.EnumRoleType;
import com.capstone_project.elderly_platform.pojos.Account;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class AccountSpecification {

    public static Specification<Account> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("deleted"), false);
    }

    public static Specification<Account> filterByRole(EnumRoleType roleType) {
        return (root, query, criteriaBuilder) -> {
            if (roleType == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("role").get("roleName"), roleType);
        };
    }

    public static Specification<Account> excludeAdminRole() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("role").get("roleName"),
                EnumRoleType.ROLE_ADMIN);
    }

    public static Specification<Account> searchByEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            String pattern = "%" + email.toLowerCase() + "%";
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern);
        };
    }

    public static Specification<Account> filterByLockedStatus(Boolean isLocked) {
        return (root, query, criteriaBuilder) -> {
            if (isLocked == null) {
                return criteriaBuilder.conjunction();
            }
            if (isLocked) {
                // Locked: enabled = false OR nonLocked = false
                return criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("enabled"), false),
                        criteriaBuilder.equal(root.get("nonLocked"), false));
            } else {
                // Not locked: enabled = true AND nonLocked = true
                return criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("enabled"), true),
                        criteriaBuilder.equal(root.get("nonLocked"), true));
            }
        };
    }

    public static Specification<Account> filterByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return criteriaBuilder.conjunction();
            }
            if (startDate != null && endDate != null) {
                return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
            }
            if (startDate != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
        };
    }
}