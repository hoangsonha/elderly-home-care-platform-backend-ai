package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.AccountRegisterRequest;
import com.capstone_project.elderly_platform.dtos.request.AccountVerificationRequest;
import com.capstone_project.elderly_platform.dtos.response.TokenResponse;
import com.capstone_project.elderly_platform.pojos.Account;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public interface AccountService {

    boolean registerAccount(AccountRegisterRequest accountRegisterRequest);

    TokenResponse verificationUser(AccountVerificationRequest request);

    TokenResponse refreshToken(String refreshToken);

    TokenResponse login(String email, String password);

    boolean logout(HttpServletRequest request);

    Account getAccountById(UUID id);
}
