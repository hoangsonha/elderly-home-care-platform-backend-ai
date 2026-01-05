package com.capstone_project.elderly_platform.dtos.response.externals;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentLinkWithQRCodeResponse {
  private String checkoutUrl;
  private String qrCodeBase64; // Base64 encoded PNG image
  private Long orderCode;
  private Long amount;
  private String description;
  private String productName;
  private UUID paymentId;
  private UUID careServiceId;
}

