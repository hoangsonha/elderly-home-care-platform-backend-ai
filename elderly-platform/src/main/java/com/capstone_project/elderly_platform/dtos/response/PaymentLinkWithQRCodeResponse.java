package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkWithQRCodeResponse {
  private String checkoutUrl;
  private String qrCodeBase64; // Base64 encoded PNG image
  private Long orderCode;
  private Long amount;
  private String description;
  private String productName;
}

