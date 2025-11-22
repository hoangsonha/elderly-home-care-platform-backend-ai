package com.capstone_project.elderly_platform.dtos.request.externals;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EstimatePayoutRequest {
    Long amount;
    String bankCode; // toBin - required
    String accountNumber; // toAccountNumber - optional cho estimate
    String description; // optional
    String referenceId; // optional
    String payoutReferenceId; // optional - referenceId cho payout item
    List<String> category; // optional
    Boolean validateDestination;
}
