package com.capstone_project.elderly_platform.validators;

import com.capstone_project.elderly_platform.configurations.ServicePackageProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DurationHoursValidator implements ConstraintValidator<ValidDurationHours, Integer> {

    private final ServicePackageProperties servicePackageProperties;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }

        var allowedValues = servicePackageProperties.getAllowedDurationHoursList();
        boolean isValid = allowedValues.contains(value);

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Duration hours must be one of: " + allowedValues).addConstraintViolation();
        }

        return isValid;
    }
}
