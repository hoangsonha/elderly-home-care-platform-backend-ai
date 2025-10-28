package com.capstone_project.elderly_platform.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ParseEnumException extends RuntimeException {

    private String message;

    public ParseEnumException(String message) {
        this.message = message;
    }

}
