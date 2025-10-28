package com.capstone_project.elderly_platform.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EntityNotFoundException extends RuntimeException {

    private String message;

    public EntityNotFoundException(String message) {
        this.message = message;
    }

}
