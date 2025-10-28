package com.capstone_project.elderly_platform.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ElementExistException extends RuntimeException {

    private String message;

    public ElementExistException(String message) {
        this.message = message;
    }

}
