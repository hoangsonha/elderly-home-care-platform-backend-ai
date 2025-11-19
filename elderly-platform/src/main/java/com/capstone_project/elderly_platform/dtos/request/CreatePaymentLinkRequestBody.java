package com.capstone_project.elderly_platform.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreatePaymentLinkRequestBody {
  private String productName;
  private String description;
  private String returnUrl;
  private int price;
  private String cancelUrl;
}
