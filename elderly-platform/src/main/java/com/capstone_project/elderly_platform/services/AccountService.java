package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.AccountRegisterRequest;
import com.capstone_project.elderly_platform.dtos.request.AccountVerificationRequest;
import com.capstone_project.elderly_platform.dtos.request.ForgotPasswordRequest;
import com.capstone_project.elderly_platform.dtos.request.ResendCodeVerifyRequest;
import com.capstone_project.elderly_platform.dtos.request.ResetPasswordRequest;
import com.capstone_project.elderly_platform.dtos.request.VerifyForgotPasswordCodeRequest;
import com.capstone_project.elderly_platform.dtos.response.TokenResponse;
import com.capstone_project.elderly_platform.pojos.Account;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public interface AccountService {

    boolean registerAccount(AccountRegisterRequest accountRegisterRequest);

    TokenResponse verificationUser(AccountVerificationRequest request);

    boolean resendCodeVerify(ResendCodeVerifyRequest request);

    TokenResponse refreshToken(String refreshToken);

    TokenResponse login(String email, String password);

    boolean logout(HttpServletRequest request);

    Account getAccountById(UUID id);

    // Forgot password APIs
    boolean sendForgotPasswordCode(ForgotPasswordRequest request);

    boolean verifyForgotPasswordCode(VerifyForgotPasswordCodeRequest request);

    boolean resetPassword(ResetPasswordRequest request);
}
