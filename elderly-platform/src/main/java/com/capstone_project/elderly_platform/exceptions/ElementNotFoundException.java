package com.capstone_project.elderly_platform.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ElementNotFoundException extends RuntimeException {

    private String message;

    public ElementNotFoundException(String message) {
        this.message = message;
    }

}
