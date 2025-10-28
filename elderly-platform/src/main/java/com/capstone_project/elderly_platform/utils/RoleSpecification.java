package com.capstone_project.elderly_platform.utils;

import com.capstone_project.elderly_platform.pojos.Role;
import org.springframework.data.jpa.domain.Specification;

public class RoleSpecification {

    public static Specification<Role> searchByField(String field, String value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null || value.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + value.toLowerCase() + "%";

            switch (field) {
                case "model":
                    return criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern);
                case "manufacturer":
                    return criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern);
                default:
                    return null;
            }
        };
    }

}
