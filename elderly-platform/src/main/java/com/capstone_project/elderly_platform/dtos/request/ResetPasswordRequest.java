package com.capstone_project.elderly_platform.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {
    @Email(message = "Invalid email")
    @NotBlank(message = "Please enter email")
    @Size(min = 10, max = 255, message = "Email must be between 10 and 255 characters")
    String email;

    @NotBlank(message = "Please enter verification code")
    @Size(min = 6, max = 6, message = "Verification code must be 6 digits")
    String code;

    @NotBlank(message = "Please enter new password")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    String newPassword;
}


