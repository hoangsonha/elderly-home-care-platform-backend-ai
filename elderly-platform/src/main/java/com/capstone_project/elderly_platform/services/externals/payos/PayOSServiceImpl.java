package com.capstone_project.elderly_platform.services.externals.payos;

import com.capstone_project.elderly_platform.dtos.request.externals.CreatePaymentLinkRequestBody;
import com.capstone_project.elderly_platform.dtos.request.externals.CreatePaymentSuccess;
import com.capstone_project.elderly_platform.dtos.request.externals.CreatePayoutRequest;
import com.capstone_project.elderly_platform.dtos.request.externals.EstimatePayoutRequest;
import com.capstone_project.elderly_platform.dtos.response.ApiResponse;
import com.capstone_project.elderly_platform.dtos.response.externals.PaymentLinkWithQRCodeResponse;
import com.capstone_project.elderly_platform.enums.EnumPaymentStatusType;
import com.capstone_project.elderly_platform.enums.EnumPayoutStatusType;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.pojos.*;
import com.capstone_project.elderly_platform.repositories.*;
import com.capstone_project.elderly_platform.utils.QRCodeGeneration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import vn.payos.PayOS;
import vn.payos.core.FileDownloadResponse;
import vn.payos.core.Page;
import vn.payos.exception.APIException;
import vn.payos.model.v1.payouts.GetPayoutListParams;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.PayoutApprovalState;
import vn.payos.model.v1.payouts.PayoutRequests;
import vn.payos.model.v1.payouts.batch.PayoutBatchItem;
import vn.payos.model.v1.payouts.batch.PayoutBatchRequest;
import vn.payos.model.v1.payoutsAccount.PayoutAccountInfo;
import vn.payos.model.v2.paymentRequests.*;
import vn.payos.model.v2.paymentRequests.invoices.InvoicesInfo;
import vn.payos.model.webhooks.ConfirmWebhookResponse;
import vn.payos.model.webhooks.WebhookData;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSServiceImpl implements PayOSService {

    @Qualifier("payOSPayout")
    private final PayOS payOSPayout;
    private final PayOS payOS;
    private final PaymentRepository paymentRepository;
    private final PayoutRepository payoutRepository;
    private final PayoutBatchRepository payoutBatchRepository;
    private final CareServiceRepository careServiceRepository;
    private final QRCodeGeneration qrCodeGeneration;
    private final RestTemplate restTemplate;

    @Value("${payos.url-success}")
    private String successUrlFE;

    @Value("${payos.url-cancel}")
    private String cancelUrlFE;

    @Value("${payos.payout-client-id}")
    private String payoutClientId;

    @Value("${payos.payout-api-key}")
    private String payoutApiKey;

    @Value("${payos.payout-checksum-key}")
    private String payoutChecksumKey;

    @Transactional
    @Override
    public PaymentLinkWithQRCodeResponse createPaymentLink(CreatePaymentLinkRequestBody requestBody) {
        try {

            CareService careService = careServiceRepository
                    .findByCareServiceIdAndDeletedIsFalse(requestBody.getCareServiceId());

            if (careService == null) {
                throw new BadRequestException("CareService not found");
            }

            // Extract packageName from careServiceSnapshot JSONB
            String packageName = "Gói chăm sóc";
            String descriptionPackage = "Thanh toán gói dịch vụ ";
            int pricePackage = 0;
            try {
                if (careService.getCareServiceSnapshot() != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    CareServiceSnapshot snapshot = mapper.readValue(
                            careService.getCareServiceSnapshot(),
                            CareServiceSnapshot.class);

                    if (snapshot.getServicePackage() != null) {
                        if (snapshot.getServicePackage().getPackageName() != null) {
                            packageName = snapshot.getServicePackage().getPackageName();
                        }
                        if (snapshot.getServicePackage().getPrice() != null) {
                            pricePackage = snapshot.getServicePackage().getPrice().intValue();
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse careServiceSnapshot: {}", e.getMessage());
                // Use default packageName
            }

            final String productName = "Thanh toán cho gói " + packageName;
            final String description = descriptionPackage;
            final String returnUrl = successUrlFE;
            final String cancelUrl = cancelUrlFE;
            final long price = pricePackage;
            final long orderCode = System.currentTimeMillis() / 1000;

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(price)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .item(PaymentLinkItem.builder().name(productName).price(price).quantity(1).build())
                    .build();

            CreatePaymentLinkResponse data = payOS.paymentRequests().create(paymentData);
            String checkoutUrl = data.getCheckoutUrl();
            String vietQRData = data.getQrCode(); // Lấy VietQR data từ response

            // Log để debug
            System.out.println("=== CreatePaymentLinkResponse ===");
            System.out.println("Checkout URL: " + checkoutUrl);
            System.out.println("VietQR Data: " + vietQRData);

            // Generate QR code từ VietQR data (app banking sẽ quét được)
            String qrCodeBase64;
            if (vietQRData != null && !vietQRData.isEmpty()) {
                // Dùng VietQR data để generate QR code
                qrCodeBase64 = qrCodeGeneration.generateQRCodeBase64(vietQRData);
                System.out.println("QR Code generated from VietQR data, length: " + qrCodeBase64.length());
            } else {
                // Fallback: dùng checkoutUrl nếu không có VietQR data
                qrCodeBase64 = qrCodeGeneration.generateQRCodeBase64(checkoutUrl);
                System.out.println("WARNING: No VietQR data, using checkoutUrl instead");
            }

            CareSeekerProfile careSeekerProfile = careService.getCareSeekerProfile();

            if (careSeekerProfile == null) {
                throw new BadRequestException("CareSeekerProfile not found");
            }

            Payment payment = Payment.builder()
                    .paymentCode(String.valueOf(orderCode))
                    .paymentMethod("PayOS")
                    .amount((double) price)
                    .status(EnumPaymentStatusType.PENDING)
                    .seekerProfile(careSeekerProfile)
                    .careService(careService)
                    .build();

            Payment savedPayment = paymentRepository.save(payment);

            PaymentLinkWithQRCodeResponse response = new PaymentLinkWithQRCodeResponse(
                    checkoutUrl, qrCodeBase64, orderCode, price, description, productName, savedPayment.getPaymentId());

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Transactional
    @Override
    public PaymentLink getOrderStatus(long orderId, CreatePaymentSuccess requestBody) {
        try {
            PaymentLink order = payOS.paymentRequests().get(orderId);

            if (order.getStatus().equals(PaymentLinkStatus.PAID)) {
                Payment payment = paymentRepository.findById(requestBody.getPaymentId())
                        .orElseThrow(() -> new BadRequestException("Payment not found"));

                LocalDateTime now = LocalDateTime.now();

                payment.setStatus(EnumPaymentStatusType.SUCCESS);
                payment.setGatewayResponseData(new ObjectMapper().writeValueAsString(order));
                payment.setPaidAt(now);
                paymentRepository.save(payment);

                CareService careService = careServiceRepository
                        .findByCareServiceIdAndDeletedIsFalse(requestBody.getCareServiceId());

                if (careService == null) {
                    throw new BadRequestException("CareService not found");
                }

                // Get or create PayoutBatch for this caregiver's current month
                PayoutBatch payoutBatch = getOrCreatePayoutBatch(careService.getCaregiverProfile(), now);

                // Create Payout and link to PayoutBatch
                com.capstone_project.elderly_platform.pojos.Payout payout = com.capstone_project.elderly_platform.pojos.Payout
                        .builder()
                        .payoutCode("payout_" + careService.getCareServiceId())
                        .caregiverEarnings(careService.getCaregiverEarnings())
                        .serviceDate(now.toLocalDate())
                        .status(EnumPayoutStatusType.PENDING)
                        .systemRevenue(careService.getTotalPrice() - careService.getCaregiverEarnings())
                        .totalAmount(careService.getTotalPrice())
                        .systemFeePercentage(careService.getSystemFeePercentage())
                        .careService(careService)
                        .caregiverProfile(careService.getCaregiverProfile())
                        .payoutBatch(payoutBatch)
                        .build();

                // Set timestamps manually
                payout.setCreatedAt(now);
                payout.setUpdatedAt(now);
                payout.setDeleted(false);

                // Save payout
                com.capstone_project.elderly_platform.pojos.Payout savedPayout = payoutRepository.save(payout);

                // Update PayoutBatch totals
                updatePayoutBatchTotals(payoutBatch, savedPayout);
            }

            return order;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get existing PayoutBatch or create new one for caregiver's current month
     */
    private PayoutBatch getOrCreatePayoutBatch(CaregiverProfile caregiverProfile, LocalDateTime now) {
        int month = now.getMonthValue();
        int year = now.getYear();

        // Try to find existing batch for this caregiver and month
        String batchCode = "batch_" + caregiverProfile.getCaregiverProfileId() + "_" + year + "_"
                + String.format("%02d", month);

        PayoutBatch existingBatch = payoutBatchRepository.findByBatchCode(batchCode);

        if (existingBatch != null && !existingBatch.isDeleted()) {
            return existingBatch;
        }

        // Create new batch
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1);

        PayoutBatch newBatch = PayoutBatch.builder()
                .batchCode(batchCode)
                .payoutMonth(month)
                .payoutYear(year)
                .totalBookings(0)
                .totalServiceAmount(0.0)
                .totalSystemFeeAmount(0.0)
                .totalCaregiverEarnings(0.0)
                .status(com.capstone_project.elderly_platform.enums.EnumPayoutBatchStatusType.PENDING)
                .startDate(firstDayOfMonth)
                .endDate(lastDayOfMonth)
                .scheduledAt(lastDayOfMonth.plusDays(5)) // Schedule payout 5 days after month end
                .caregiverProfile(caregiverProfile)
                .build();

        // Set timestamps manually
        newBatch.setCreatedAt(now);
        newBatch.setUpdatedAt(now);
        newBatch.setDeleted(false);

        return payoutBatchRepository.save(newBatch);
    }

    /**
     * Update PayoutBatch totals when new Payout is added
     */
    private void updatePayoutBatchTotals(PayoutBatch payoutBatch,
            com.capstone_project.elderly_platform.pojos.Payout payout) {
        // Increment totals
        payoutBatch.setTotalBookings(payoutBatch.getTotalBookings() + 1);
        payoutBatch.setTotalServiceAmount(payoutBatch.getTotalServiceAmount() + payout.getTotalAmount());
        payoutBatch.setTotalSystemFeeAmount(payoutBatch.getTotalSystemFeeAmount() + payout.getSystemRevenue());
        payoutBatch.setTotalCaregiverEarnings(payoutBatch.getTotalCaregiverEarnings() + payout.getCaregiverEarnings());

        // Update timestamp
        payoutBatch.setUpdatedAt(LocalDateTime.now());

        payoutBatchRepository.save(payoutBatch);
    }

    @Override
    public WebhookData payosTransferHandler(Object body) throws IllegalArgumentException {
        try {
            WebhookData data = payOS.webhooks().verify(body);
            System.out.println(data);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public CreatePaymentLinkResponse createOrderLink(CreatePaymentLinkRequestBody requestBody) {
        try {

            CareService careService = careServiceRepository
                    .findByCareServiceIdAndDeletedIsFalse(requestBody.getCareServiceId());

            if (careService == null) {
                throw new BadRequestException("CareService not found");
            }

            // Extract packageName from careServiceSnapshot JSONB
            String packageName = "Gói chăm sóc";
            String descriptionPackage = "Thanh toán gói dịch vụ ";
            int pricePackage = 0;
            try {
                if (careService.getCareServiceSnapshot() != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    CareServiceSnapshot snapshot = mapper.readValue(
                            careService.getCareServiceSnapshot(),
                            CareServiceSnapshot.class);

                    if (snapshot.getServicePackage() != null) {
                        if (snapshot.getServicePackage().getPackageName() != null) {
                            packageName = snapshot.getServicePackage().getPackageName();
                        }
                        if (snapshot.getServicePackage().getPrice() != null) {
                            pricePackage = snapshot.getServicePackage().getPrice().intValue();
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse careServiceSnapshot: {}", e.getMessage());
                // Use default packageName
            }

            final String productName = "Thanh toán cho gói " + packageName;
            final String description = descriptionPackage;
            final String returnUrl = successUrlFE;
            final String cancelUrl = cancelUrlFE;
            final long price = pricePackage;
            final long orderCode = System.currentTimeMillis() / 1000;

            PaymentLinkItem item = PaymentLinkItem.builder().name(productName).quantity(1).price(price).build();

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .description(description)
                    .amount(price)
                    .item(item)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .build();

            CreatePaymentLinkResponse data = payOS.paymentRequests().create(paymentData);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public PaymentLink cancelOrderLink(long orderId) {
        try {
            PaymentLink order = payOS.paymentRequests().cancel(orderId, "change my mind");
            return order;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ConfirmWebhookResponse confirmWebhook(Map<String, String> requestBody) {
        try {
            ConfirmWebhookResponse result = payOS.webhooks().confirm(requestBody.get("webhookUrl"));
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public InvoicesInfo getInvoiceInfo(long orderId) {
        try {
            InvoicesInfo invoicesInfo = payOS.paymentRequests().invoices().get(orderId);
            return invoicesInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ResponseEntity<?> downloadInvoice(long orderId, String invoiceId) {
        try {
            FileDownloadResponse invoiceFile = payOS.paymentRequests().invoices().download(invoiceId, orderId);

            if (invoiceFile == null || invoiceFile.getData() == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("invoice not found or empty"));
            }

            ByteArrayResource resource = new ByteArrayResource(invoiceFile.getData());

            HttpHeaders headers = new HttpHeaders();
            String contentType = invoiceFile.getContentType() == null
                    ? MediaType.APPLICATION_PDF_VALUE
                    : invoiceFile.getContentType();
            headers.set(HttpHeaders.CONTENT_TYPE, contentType);
            headers.set(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + invoiceFile.getFilename() + "\"");
            if (invoiceFile.getSize() != null) {
                headers.setContentLength(invoiceFile.getSize());
            }

            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (APIException e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(e.getErrorDesc().orElse(e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @Override
    public PayoutAccountInfo getBalanceInfo() {
        try {
            PayoutAccountInfo accountInfo = payOSPayout.payoutsAccount().balance();
            return accountInfo;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Payout> getAllPayouts(String referenceId, String approvalState, List<String> category, String fromDate,
            String toDate, Integer limit, Integer offset) {
        try {
            GetPayoutListParams.GetPayoutListParamsBuilder paramsBuilder = GetPayoutListParams.builder()
                    .referenceId(referenceId)
                    .category(category)
                    .limit(limit)
                    .offset(offset);
            if (fromDate != null && !fromDate.isEmpty()) {
                paramsBuilder.fromDate(fromDate);
            }
            if (toDate != null && !toDate.isEmpty()) {
                paramsBuilder.toDate(toDate);
            }

            PayoutApprovalState parsedApprovalState = null;
            if (approvalState != null && !approvalState.isEmpty()) {
                try {
                    parsedApprovalState = PayoutApprovalState.valueOf(approvalState.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Invalid approval state: " + approvalState);
                }
                paramsBuilder.approvalState(parsedApprovalState);
            }

            GetPayoutListParams params = paramsBuilder.build();

            List<Payout> data = new ArrayList<>();
            Page<Payout> page = payOSPayout.payouts().list(params);
            page.autoPager().stream().forEach(data::add);
            return data;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Payout getPayoutById(String payoutId) {
        try {
            Payout payout = payOSPayout.payouts().get(payoutId);
            return payout;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Payout createPayout(CreatePayoutRequest request) {
        try {
            // Validate required fields
            if (request.getAmount() == null || request.getAmount() < 1000) {
                throw new BadRequestException("Payout amount must be greater than 1000 VND");
            }

            if (request.getAccountNumber() == null || request.getAccountNumber().trim().isEmpty()) {
                throw new BadRequestException("Account number is required");
            }

            if (request.getAccountName() == null || request.getAccountName().trim().isEmpty()) {
                throw new BadRequestException("Account name is required");
            }

            if (request.getBankCode() == null || request.getBankCode().trim().isEmpty()) {
                throw new BadRequestException("Bank code is required");
            }

            // Tạo Map với field names mà PayOS API yêu cầu (camelCase)
            Map<String, Object> payoutData = new HashMap<>();
            payoutData.put("amount", request.getAmount());
            payoutData.put("toAccountNumber", request.getAccountNumber().trim());
            payoutData.put("toBin", request.getBankCode().trim());
            payoutData.put("description",
                    request.getDescription() != null ? request.getDescription().trim() : "Chuyển tiền");

            // Set referenceId
            if (request.getReferenceId() != null && !request.getReferenceId().isEmpty()) {
                payoutData.put("referenceId", request.getReferenceId());
            } else {
                payoutData.put("referenceId", "payout_" + (System.currentTimeMillis() / 1000));
            }

            // Convert Map sang PayoutRequests object
            ObjectMapper mapper = new ObjectMapper();
            PayoutRequests payoutRequest = mapper.convertValue(payoutData, PayoutRequests.class);

            // Log để debug
            System.out.println("=== Payout Request ===");
            System.out.println("Amount: " + payoutData.get("amount"));
            System.out.println("To Account Number: " + payoutData.get("toAccountNumber"));
            System.out.println("To BIN: " + payoutData.get("toBin"));
            System.out.println("Description: " + payoutData.get("description"));
            System.out.println("Reference ID: " + payoutData.get("referenceId"));

            Payout payout = payOS.payouts().create(payoutRequest);
            return payout;

        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Failed to create payout";
            System.err.println("=== Lỗi khi tạo payout ===");
            System.err.println("Error: " + errorMessage);
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
            return null;
        }
    }

    @Override
    public Payout createPayoutBatch(PayoutBatchRequest payoutBatchRequest) {
        try {
            if (payoutBatchRequest.getReferenceId() == null || payoutBatchRequest.getReferenceId().isEmpty()) {
                payoutBatchRequest.setReferenceId("payout_" + (System.currentTimeMillis() / 1000));
            }

            List<PayoutBatchItem> payoutsList = payoutBatchRequest.getPayouts();
            if (payoutsList == null) {
                throw new BadRequestException("Payouts list is empty");
            }
            for (int i = 0; i < payoutsList.size(); i++) {
                PayoutBatchItem batchItem = payoutsList.get(i);
                if (batchItem.getReferenceId() == null) {
                    batchItem.setReferenceId("payout_" + (System.currentTimeMillis() / 1000) + "_" + i);
                }
            }

            Payout payout = payOSPayout.payouts().batch().create(payoutBatchRequest);
            return payout;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<String, Object> getEstimatedFees(EstimatePayoutRequest request) {
        try {
            // Validate required fields
            if (request.getAmount() == null || request.getAmount() < 1000) {
                throw new BadRequestException("The minimum amount is 1,000 VND");
            }

            if (request.getBankCode() == null || request.getBankCode().trim().isEmpty()) {
                throw new BadRequestException("Bank code is required");
            }

            // Tạo batch request với 1 payout để estimate
            PayoutBatchRequest batchRequest = new PayoutBatchRequest();

            // Set referenceId
            if (request.getReferenceId() != null && !request.getReferenceId().isEmpty()) {
                batchRequest.setReferenceId(request.getReferenceId());
            } else {
                batchRequest.setReferenceId("estimate_" + (System.currentTimeMillis() / 1000));
            }

            // Set category nếu có
            if (request.getCategory() != null && !request.getCategory().isEmpty()) {
                batchRequest.setCategory(request.getCategory());
            }

            // Note: validateDestination có thể không có trong PayoutBatchRequest
            // Nếu PayOS SDK hỗ trợ, uncomment dòng dưới:
            // batchRequest.setValidateDestination(request.getValidateDestination() != null
            // ? request.getValidateDestination() : true);

            // Tạo payout item
            PayoutBatchItem payoutItem = new PayoutBatchItem();
            payoutItem.setAmount(request.getAmount());
            payoutItem.setToBin(request.getBankCode().trim());

            // Nếu có accountNumber thì set, không thì chỉ cần toBin để estimate
            if (request.getAccountNumber() != null && !request.getAccountNumber().trim().isEmpty()) {
                payoutItem.setToAccountNumber(request.getAccountNumber().trim());
            }

            if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
                payoutItem.setDescription(request.getDescription().trim());
            } else {
                // TEST: Dùng ASCII đơn giản để tránh lỗi encoding khi tính signature
                payoutItem.setDescription("Estimate fee");
            }

            if (request.getPayoutReferenceId() != null && !request.getPayoutReferenceId().isEmpty()) {
                payoutItem.setReferenceId(request.getPayoutReferenceId());
            } else {
                payoutItem.setReferenceId("estimate_payout_" + (System.currentTimeMillis() / 1000));
            }

            List<PayoutBatchItem> payoutsList = new ArrayList<>();
            payoutsList.add(payoutItem);
            batchRequest.setPayouts(payoutsList);

            // Gọi API estimate trực tiếp qua HTTP
            String apiUrl = "https://api-merchant.payos.vn/v1/payouts/estimate-credit";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", payoutClientId);
            headers.set("x-api-key", payoutApiKey);
            // Note: x-signature có thể cần tính toán từ request body + checksum key
            // Nếu PayOS yêu cầu, cần implement signature generation

            // Convert batchRequest to Map for JSON serialization
            ObjectMapper mapper = new ObjectMapper();
            // Disable pretty printing để không có spaces
            mapper.configure(SerializationFeature.INDENT_OUTPUT, false);

            @SuppressWarnings("unchecked")
            Map<String, Object> requestBody = mapper.convertValue(batchRequest, Map.class);

            // Normalize body (convert null → "")
            Map<String, Object> normalized = normalize(requestBody);

            // ⚠️ QUAN TRỌNG: Loại bỏ các field không cần thiết hoặc gây lỗi
            normalized.remove("validateDestination");

            // Loại bỏ category nếu rỗng (có thể PayOS không chấp nhận category rỗng)
            if (normalized.containsKey("category") && "".equals(normalized.get("category"))) {
                normalized.remove("category");
            }

            // ⚠️ QUAN TRỌNG: Sort TẤT CẢ keys (bao gồm nested objects trong arrays)
            // PayOS yêu cầu: payouts[0].amount, payouts[0].description,
            // payouts[0].referenceId (alphabetical)
            Map<String, Object> sortedRequestBody = sortKeysRecursively(normalized);

            // Build query string theo format PayOS (từ code mẫu)
            // Format:
            // key=encodeURIComponent(JSON.stringify(value))&key=encodeURIComponent(value)...
            String queryString = buildQueryString(sortedRequestBody);

            // Tính signature từ query string
            try {
                String signature = calculateSignature(queryString, payoutChecksumKey);

                headers.set("x-signature", signature);
            } catch (Exception e) {
                System.err.println("Lỗi khi tính signature: " + e.getMessage());
                e.printStackTrace();
            }

            // Dùng sortedRequestBody cho request
            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(sortedRequestBody, headers);

            try {
                @SuppressWarnings("unchecked")
                ResponseEntity<Map> response = restTemplate.exchange(
                        apiUrl,
                        HttpMethod.POST,
                        httpRequest,
                        Map.class);

                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                return responseBody;
            } catch (Exception e) {
                System.err.println("=== Lỗi khi gọi estimate API ===");
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Failed to estimate cost";
            System.err.println("=== Lỗi khi estimate payout ===");
            System.err.println("Error: " + errorMessage);
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
            return null;
        }
    }

    // private methods

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        String url = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80)
                || (scheme.equals("https") && serverPort != 443)) {
            url += ":" + serverPort;
        }
        url += contextPath;
        return url;
    }

    private Map<String, Object> normalize(Map<String, Object> input) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : input.keySet()) {
            Object value = input.get(key);
            if (value == null) {
                result.put(key, "");
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = normalize((Map<String, Object>) value);
                result.put(key, nestedMap);
            } else if (value instanceof List) {
                List<Object> newList = new ArrayList<>();
                for (Object item : (List<?>) value) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> normalizedItem = normalize((Map<String, Object>) item);
                        newList.add(normalizedItem);
                    } else if (item == null) {
                        newList.add("");
                    } else {
                        newList.add(item);
                    }
                }
                result.put(key, newList);
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private Map<String, Object> sortKeysRecursively(Map<String, Object> input) {
        Map<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sortedNested = sortKeysRecursively((Map<String, Object>) value);
                sorted.put(entry.getKey(), sortedNested);
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                List<Object> sortedList = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> sortedItem = sortKeysRecursively((Map<String, Object>) item);
                        sortedList.add(sortedItem);
                    } else {
                        sortedList.add(item);
                    }
                }
                sorted.put(entry.getKey(), sortedList);
            } else {
                sorted.put(entry.getKey(), value);
            }
        }
        return sorted;
    }

    /**
     * Build query string theo format PayOS (từ code mẫu documentation)
     * Format: key=encodeURIComponent(value)&key=encodeURIComponent(value)...
     * - Array/Object: JSON.stringify() rồi mới encodeURIComponent()
     * - null/undefined: ""
     * - Top-level keys: sorted alphabetically
     */
    private String buildQueryString(Map<String, Object> sortedMap) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        ObjectMapper mapper = new ObjectMapper();

        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            first = false;

            String key = entry.getKey();
            Object value = entry.getValue();

            try {
                String encodedKey = urlEncode(key);
                String encodedValue;

                if (value == null) {
                    encodedValue = "";
                } else if (value instanceof List || (value instanceof Map)) {
                    // Array và object: JSON.stringify() rồi mới encode
                    String jsonValue = mapper.writeValueAsString(value);
                    encodedValue = urlEncode(jsonValue);
                } else {
                    encodedValue = urlEncode(String.valueOf(value));
                }

                sb.append(encodedKey).append("=").append(encodedValue);
            } catch (Exception e) {
                System.err.println("Error building query string: " + e.getMessage());
            }
        }

        return sb.toString();
    }

    /**
     * URL encode (giống encodeURIComponent trong JavaScript)
     */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * Tính x-signature cho PayOS API request
     * PayOS sử dụng HMAC SHA256 với JSON payload
     * Format: lowercase hex string
     */
    private String calculateSignature(String payload, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secretKey);
            byte[] hash = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Signature calculation failed", e);
        }
    }

    // Inner class for CareServiceSnapshot deserialization
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class CareServiceSnapshot {
        private com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO elderlyProfile;
        private com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileResponseDTO careSeekerProfile;
        private com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO caregiverProfile;
        private com.capstone_project.elderly_platform.dtos.response.ServicePackageResponseDTO servicePackage;
    }

}
