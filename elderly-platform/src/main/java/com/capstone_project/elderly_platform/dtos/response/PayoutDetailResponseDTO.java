package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutDetailResponseDTO {
    private String payoutId;                 // ID của payout
    private String payoutCode;               // Mã payout
    private Double caregiverEarnings;       // Thu nhập của caregiver từ care-service này
    private Double totalAmount;              // Tổng số tiền của care-service
    private Double systemRevenue;            // Phần thu nhập của hệ thống
    private Double systemFeePercentage;      // Phần trăm phí hệ thống
    private String serviceDate;              // Ngày thực hiện dịch vụ
    private String status;                   // Trạng thái payout (PENDING, PROCESSING, COMPLETED, FAILED)
    private String includedAt;               // Thời gian được thêm vào batch
    private String paidAt;                   // Thời gian được thanh toán
    private String careServiceId;            // ID của care-service
    private String bookingCode;              // Mã booking của care-service
    private String workDate;                 // Ngày làm việc
    private String payoutBatchId;            // ID của payout batch
    private String batchCode;                // Mã batch
}
