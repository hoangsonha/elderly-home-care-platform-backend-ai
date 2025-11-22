package com.capstone_project.elderly_platform.dtos.request.externals;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.aspectj.bridge.IMessage;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePaymentSuccess {
    @NotNull(message = "paymentId is required")
    UUID paymentId;

    @NotNull(message = "careServiceId is required")
    UUID careServiceId;
}
