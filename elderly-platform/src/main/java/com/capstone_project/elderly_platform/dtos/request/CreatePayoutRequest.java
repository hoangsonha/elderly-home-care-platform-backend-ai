package com.capstone_project.elderly_platform.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class CreatePayoutRequest {
    private Long amount;
    private String accountNumber;
    private String accountName;
    private String bankCode;
    private String description;
    private String referenceId;
}
