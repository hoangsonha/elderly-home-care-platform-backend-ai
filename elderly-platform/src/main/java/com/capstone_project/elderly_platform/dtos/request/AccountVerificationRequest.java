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

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Vui lòng nhập email")
    @Size(min = 10, max = 255, message = "Email phải từ 10 tới 255 kí tự bao gồm cả @gmail.com")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 6, max = 6, message = "Mật khẩu phải từ 6 tới 100 kí tự")
    private String verificationCode;
}
