package com.capstone_project.elderly_platform.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountVerificationRequest {

    @Email(message = "Invalid email")
    @NotBlank(message = "Please enter email")
    @Size(min = 10, max = 255, message = "Email must be between 10 and 255 characters including @gmail.com")
    private String email;

    @NotBlank(message = "Please enter verification code")
    @Size(min = 6, max = 6, message = "Verification code must be 6 characters")
    private String verificationCode;
}
