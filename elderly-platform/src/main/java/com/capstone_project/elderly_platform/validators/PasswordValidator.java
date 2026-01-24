package com.capstone_project.elderly_platform.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true; // Let @NotBlank handle null/empty validation
        }

        // Check minimum length
        if (password.length() < 8) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Password must be at least 8 characters long")
                    .addConstraintViolation();
            return false;
        }

        // Check for uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one uppercase letter")
                    .addConstraintViolation();
            return false;
        }

        // Check for lowercase letter
        if (!password.matches(".*[a-z].*")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one lowercase letter")
                    .addConstraintViolation();
            return false;
        }

        // Check for number
        if (!password.matches(".*\\d.*")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one number")
                    .addConstraintViolation();
            return false;
        }

        // Check for special character
        if (!password.matches(".*[@$!%*?&].*")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one special character (@$!%*?&)")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
