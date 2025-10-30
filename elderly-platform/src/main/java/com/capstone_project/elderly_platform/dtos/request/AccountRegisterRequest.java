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
public class AccountRegisterRequest {
    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Vui lòng nhập email")
    @Size(min = 10, max = 255, message = "Email phải từ 10 tới 255 kí tự bao gồm cả @gmail.com")
    String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 tới 100 kí tự")
    String password;

    @NotBlank(message = "Vui lòng chọn vai trò")
    String role;
}
