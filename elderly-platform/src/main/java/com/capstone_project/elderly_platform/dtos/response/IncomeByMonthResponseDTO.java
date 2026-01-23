package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeByMonthResponseDTO {
    private Integer year;                    // Năm
    private Integer month;                   // Tháng (1-12)
    private Double totalEarnings;            // Tổng thu nhập trong tháng
    private Integer totalBookings;           // Tổng số booking trong tháng
    private Double totalServiceAmount;       // Tổng số tiền dịch vụ
    private String status;                   // Trạng thái payout batch (PENDING, PROCESSING, COMPLETED, FAILED)
    private String batchCode;                // Mã batch
    private String payoutBatchId;           // ID của payout batch
    private List<PayoutDetailResponseDTO> payoutDetails;  // Chi tiết các lần thu thập trong tháng này
}
