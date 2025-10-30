package com.capstone_project.elderly_platform.utils;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import com.capstone_project.elderly_platform.pojos.Account;

public class AccountSpecification {

    public static Specification<Account> searchByField(String field, String value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null || value.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + value.toLowerCase() + "%";

            switch (field) {
                case "code":
                    return criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern);
                case "model":
                    return criteriaBuilder.like(
                            criteriaBuilder.lower(root.join("aircraftType", JoinType.LEFT).get("model")), pattern);
                case "manufacturer":
                    return criteriaBuilder.like(
                            criteriaBuilder.lower(root.join("aircraftType", JoinType.LEFT).get("manufacturer")),
                            pattern
                    );
                default:
                    return null;
            }
        };
    }

}
