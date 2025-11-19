package com.capstone_project.elderly_platform.services.externals.payos;

import com.capstone_project.elderly_platform.dtos.request.CreatePaymentLinkRequestBody;
import com.capstone_project.elderly_platform.dtos.request.CreatePayoutRequest;
import com.capstone_project.elderly_platform.dtos.response.ApiResponse;
import com.capstone_project.elderly_platform.dtos.response.PaymentLinkWithQRCodeResponse;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.utils.QRCodeGeneration;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.v2.paymentRequests.invoices.InvoicesInfo;
import vn.payos.model.webhooks.ConfirmWebhookResponse;
import vn.payos.model.webhooks.WebhookData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayOSServiceImpl implements PayOSService {

    private final PayOS payOS;

    @Qualifier("payOSPayout")
    private final PayOS payOSPayout;

    private final QRCodeGeneration qrCodeGeneration;

    @Value("${payos.url-success}")
    private String successUrlFE;

    @Value("${payos.url-cancel}")
    private String cancelUrlFE;

    @Override
    public PaymentLinkWithQRCodeResponse createPaymentLink(CreatePaymentLinkRequestBody requestBody, HttpServletRequest request) {
        try {
            final String baseUrl = getBaseUrl(request);
            final String productName = requestBody != null && requestBody.getProductName() != null
                    ? requestBody.getProductName()
                    : "Thanh toán đơn hàng";
            final String description = requestBody != null && requestBody.getDescription() != null
                    ? requestBody.getDescription()
                    : "Thanh toan don hang";
            final String returnUrl = requestBody != null && requestBody.getReturnUrl() != null
                    ? requestBody.getReturnUrl()
                    : baseUrl + successUrlFE;
            final String cancelUrl = requestBody != null && requestBody.getCancelUrl() != null
                    ? requestBody.getCancelUrl()
                    : baseUrl + cancelUrlFE;
            final long price = requestBody != null && requestBody.getPrice() > 0
                    ? requestBody.getPrice()
                    : 2000;
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

            PaymentLinkWithQRCodeResponse response = new PaymentLinkWithQRCodeResponse(
                    checkoutUrl, qrCodeBase64, orderCode, price, description, productName);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public PaymentLink getOrderStatus(long orderId) {
        try {
            PaymentLink order = payOS.paymentRequests().get(orderId);
            return order;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
            final String productName = requestBody.getProductName();
            final String description = requestBody.getDescription();
            final String returnUrl = requestBody.getReturnUrl();
            final String cancelUrl = requestBody.getCancelUrl();
            final long price = requestBody.getPrice();
            long orderCode = System.currentTimeMillis() / 1000;
            PaymentLinkItem item =
                    PaymentLinkItem.builder().name(productName).quantity(1).price(price).build();

            CreatePaymentLinkRequest paymentData =
                    CreatePaymentLinkRequest.builder()
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
            FileDownloadResponse invoiceFile =
                    payOS.paymentRequests().invoices().download(invoiceId, orderId);

            if (invoiceFile == null || invoiceFile.getData() == null) {
              return ResponseEntity.status(404).body(ApiResponse.error("invoice not found or empty"));
            }

            ByteArrayResource resource = new ByteArrayResource(invoiceFile.getData());

            HttpHeaders headers = new HttpHeaders();
            String contentType =
                    invoiceFile.getContentType() == null
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
    public List<Payout> getAllPayouts(String referenceId, String approvalState, List<String> category, String fromDate, String toDate, Integer limit, Integer offset) {
        try {
            GetPayoutListParams.GetPayoutListParamsBuilder paramsBuilder =
                    GetPayoutListParams.builder()
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
            payoutData.put("description", request.getDescription() != null ? request.getDescription().trim() : "Chuyển tiền");

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
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Tạo payout thất bại";
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

}
