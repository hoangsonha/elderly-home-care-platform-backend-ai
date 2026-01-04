package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for ending work (includes QR code for payment)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndWorkResponse {
    private UUID careServiceId;
    private String status;
    private String checkOutImageUrl;
    private String qrCodeBase64; // Base64 encoded QR code for payment
    private String checkoutUrl;
    private Long orderCode;
    private Long amount;
    private String description;
    private String productName;
    private UUID paymentId;
    private String message;
}


