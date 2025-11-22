package com.capstone_project.elderly_platform.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DurationHoursValidator.class)
@Documented
public @interface ValidDurationHours {
    
    String message() default "Duration hours must be one of the allowed values from configuration";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}


