package com.capstone_project.elderly_platform.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AuthenticationException extends RuntimeException {

    private String message;

    public AuthenticationException(String message) {
        this.message = message;
    }

}
