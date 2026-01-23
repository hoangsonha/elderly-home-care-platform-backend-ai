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
public class CaregiverIncomeResponseDTO {
    private Double totalEarnings;                    // Tổng thu nhập tất cả các tháng
    private List<IncomeByMonthResponseDTO> incomeByMonth;  // Danh sách thu nhập theo từng tháng
    private List<PayoutDetailResponseDTO> payoutDetails;   // Danh sách các lần thu thập từ care-service hoàn thành
}
