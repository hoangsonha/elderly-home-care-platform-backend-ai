package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.externals.CreatePaymentLinkRequestBody;
import com.capstone_project.elderly_platform.dtos.request.externals.CreatePaymentSuccess;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.dtos.response.externals.PaymentLinkWithQRCodeResponse;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.services.externals.payos.PayOSService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.invoices.InvoicesInfo;
import vn.payos.model.webhooks.ConfirmWebhookResponse;
import vn.payos.model.webhooks.WebhookData;

import java.util.Map;

@RequestMapping("/api/v1/payments")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment", description = "Operations related to payment management")
public class PaymentController {

    private final PayOSService payOSService;

    /*
     * user bấm thanh toán -> gọi api "POST: /api/v1/payments/create-payment-link"
     * ->
     * sau đó nhận đc response có 1 mã base64 ->
     * dùng mã base64 này chuyển qua mã QR bằng thư viện ->
     * gọi liên tục api "GET: /api/v1/payments/order/{orderId}" sau mỗi 1 hoặc 2s để
     * kiểm tra xem người dùng đã thanh toán hay chưa,
     * có 3 status là PAID (thanh toán thành công), CANCELLED (hủy), PENDING (tiếp
     * tục kiểm tra), nếu quá 30p mà chưa thanh toán
     * thì coi như hủy và gọi api "PUT: /api/v1/payments/order/{orderId}" để hủy đơn
     * hàng
     */

    // create payment link with QR code
    @PostMapping(path = "/create-payment-link")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    public ResponseEntity<ObjectResponse> createPaymentLinkWithQR(
            @Valid @RequestBody(required = false) CreatePaymentLinkRequestBody requestBody) {
        try {
            PaymentLinkWithQRCodeResponse response = payOSService.createPaymentLink(requestBody);
            return response != null
                    ? ResponseEntity.status(HttpStatus.OK).body(
                            new ObjectResponse("Success", "Create link payment with QR Code successfully", response))
                    : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ObjectResponse("Fail", "Create link payment with QR Code failed", null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ObjectResponse("Fail", "Create link payment with QR Code failed. " + e.getMessage(), null));
        }
    }

    // check status for order
    @PostMapping(path = "/order/{orderId}")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    public ResponseEntity<ObjectResponse> getOrderById(@PathVariable("orderId") long orderId,
                                                       @RequestBody CreatePaymentSuccess requestBody) {
        try {
            PaymentLink response = payOSService.getOrderStatus(orderId, requestBody);
            return response != null
                    ? ResponseEntity.status(HttpStatus.OK)
                            .body(new ObjectResponse("Success", "Check status order successfully", response))
                    : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ObjectResponse("Fail", "Check status order failed", null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Check status order failed. " + e.getMessage(), null));
        }
    }

    // cancel order
    @PutMapping(path = "/order/{orderId}")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    public ResponseEntity<ObjectResponse> cancelOrder(@PathVariable("orderId") long orderId) {
        try {
            PaymentLink response = payOSService.cancelOrderLink(orderId);
            return response != null
                    ? ResponseEntity.status(HttpStatus.OK)
                            .body(new ObjectResponse("Success", "Cancel order successfully", response))
                    : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ObjectResponse("Fail", "Cancel order failed", null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Cancel order failed. " + e.getMessage(), null));
        }
    }

    // payments APIs
    @PostMapping(path = "/payos_transfer_handler")
    public ResponseEntity<ObjectResponse> payosTransferHandler(@RequestBody Object body) {
        try {
            WebhookData response = payOSService.payosTransferHandler(body);
            return response != null
                    ? ResponseEntity.status(HttpStatus.OK)
                            .body(new ObjectResponse("Success", "PayOS transfer handler successfully", response))
                    : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ObjectResponse("Fail", "PayOS transfer handler failed", null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "PayOS transfer handler failed. " + e.getMessage(), null));
        }
    }

    // order APIs

    @PostMapping(path = "/order/create")
    public ResponseEntity<ObjectResponse> creatOrderLink(
            @RequestBody CreatePaymentLinkRequestBody requestBody) {
        try {
            CreatePaymentLinkResponse response = payOSService.createOrderLink(requestBody);
            return response != null
                    ? ResponseEntity.status(HttpStatus.OK)
                            .body(new ObjectResponse("Success", "Create order link successfully", response))
                    : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ObjectResponse("Fail", "Create order link failed", null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Create order link failed. " + e.getMessage(), null));
        }
    }

    @PostMapping(path = "/order/confirm-webhook")
    public ResponseEntity<ObjectResponse> confirmWebhook(@RequestBody Map<String, String> requestBody) {
        try {
            ConfirmWebhookResponse response = payOSService.confirmWebhook(requestBody);
            return response != null
                    ? ResponseEntity.status(HttpStatus.OK)
                            .body(new ObjectResponse("Success", "Confirm webhook successfully", response))
                    : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ObjectResponse("Fail", "Confirm webhook failed", null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Confirm webhook failed. " + e.getMessage(), null));
        }
    }

    @GetMapping(path = "/order/{orderId}/invoices")
    public ResponseEntity<ObjectResponse> retrieveInvoices(@PathVariable("orderId") long orderId) {
        try {
            InvoicesInfo response = payOSService.getInvoiceInfo(orderId);
            return response != null
                    ? ResponseEntity.status(HttpStatus.OK)
                            .body(new ObjectResponse("Success", "Get invoices successfully", response))
                    : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ObjectResponse("Fail", "Get invoices failed", null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Get invoices failed. " + e.getMessage(), null));
        }
    }

    @GetMapping(path = "/order/{orderId}/invoices/{invoiceId}/download")
    public ResponseEntity<?> downloadInvoice(
            @PathVariable("orderId") long orderId, @PathVariable("invoiceId") String invoiceId) {
        try {
            ResponseEntity<?> response = payOSService.downloadInvoice(orderId, invoiceId);
            return response != null
                    ? ResponseEntity.status(HttpStatus.OK)
                            .body(new ObjectResponse("Success", "Download invoices successfully", response))
                    : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ObjectResponse("Fail", "Download invoices failed", null));
        } catch (ElementNotFoundException e) {
            log.error("Error found : {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Download invoices failed. " + e.getMessage(), null));
        }
    }

    // api mẫu lấy từ github: "https://github.com/payOSHQ/payos-demo-java-spring#"

    // // payments APIs
    //
    // @PostMapping(path = "/payos_transfer_handler")
    // public ApiResponse<WebhookData> payosTransferHandler(@RequestBody Object
    // body)
    // throws JsonProcessingException, IllegalArgumentException {
    // try {
    // WebhookData data = payOS.webhooks().verify(body);
    // System.out.println(data);
    // return ApiResponse.success("Webhook delivered", data);
    // } catch (Exception e) {
    // e.printStackTrace();
    // return ApiResponse.error(e.getMessage());
    // }
    // }
    //
    //
    // // order APIs
    //
    // @PostMapping(path = "/order/create")
    // public ApiResponse<CreatePaymentLinkResponse> createPaymentLink(
    // @RequestBody CreatePaymentLinkRequestBody RequestBody) {
    // try {
    // final String productName = RequestBody.getProductName();
    // final String description = RequestBody.getDescription();
    // final String returnUrl = RequestBody.getReturnUrl();
    // final String cancelUrl = RequestBody.getCancelUrl();
    // final long price = RequestBody.getPrice();
    // long orderCode = System.currentTimeMillis() / 1000;
    // PaymentLinkItem item =
    // PaymentLinkItem.builder().name(productName).quantity(1).price(price).build();
    //
    // CreatePaymentLinkRequest paymentData =
    // CreatePaymentLinkRequest.builder()
    // .orderCode(orderCode)
    // .description(description)
    // .amount(price)
    // .item(item)
    // .returnUrl(returnUrl)
    // .cancelUrl(cancelUrl)
    // .build();
    //
    // CreatePaymentLinkResponse data = payOS.paymentRequests().create(paymentData);
    // return ApiResponse.success(data);
    // } catch (Exception e) {
    // e.printStackTrace();
    // return ApiResponse.error("fail");
    // }
    // }
    //
    //
    // // *
    // @GetMapping(path = "/order/{orderId}")
    // public ApiResponse<PaymentLink> getOrderById(@PathVariable("orderId") long
    // orderId) {
    // try {
    // PaymentLink order = payOS.paymentRequests().get(orderId);
    // return ApiResponse.success("ok", order);
    // } catch (Exception e) {
    // e.printStackTrace();
    // return ApiResponse.error(e.getMessage());
    // }
    // }
    //
    // @PutMapping(path = "/order/{orderId}")
    // public ApiResponse<PaymentLink> cancelOrder(@PathVariable("orderId") long
    // orderId) {
    // try {
    // PaymentLink order = payOS.paymentRequests().cancel(orderId, "change my
    // mind");
    // return ApiResponse.success("ok", order);
    // } catch (Exception e) {
    // e.printStackTrace();
    // return ApiResponse.error(e.getMessage());
    // }
    // }
    //
    // @PostMapping(path = "/order/confirm-webhook")
    // public ApiResponse<ConfirmWebhookResponse> confirmWebhook(
    // @RequestBody Map<String, String> requestBody) {
    // try {
    // ConfirmWebhookResponse result =
    // payOS.webhooks().confirm(requestBody.get("webhookUrl"));
    // return ApiResponse.success("ok", result);
    // } catch (Exception e) {
    // e.printStackTrace();
    // return ApiResponse.error(e.getMessage());
    // }
    // }
    //
    // @GetMapping(path = "/order/{orderId}/invoices")
    // public ApiResponse<InvoicesInfo> retrieveInvoices(@PathVariable("orderId")
    // long orderId) {
    // try {
    // InvoicesInfo invoicesInfo = payOS.paymentRequests().invoices().get(orderId);
    // return ApiResponse.success("ok", invoicesInfo);
    // } catch (Exception e) {
    // e.printStackTrace();
    // return ApiResponse.error(e.getMessage());
    // }
    // }
    //
    // @GetMapping(path = "/order/{orderId}/invoices/{invoiceId}/download")
    // public ResponseEntity<?> downloadInvoice(
    // @PathVariable("orderId") long orderId, @PathVariable("invoiceId") String
    // invoiceId) {
    // try {
    // FileDownloadResponse invoiceFile =
    // payOS.paymentRequests().invoices().download(invoiceId, orderId);
    //
    // if (invoiceFile == null || invoiceFile.getData() == null) {
    // return ResponseEntity.status(404).body(ApiResponse.error("invoice not found
    // or empty"));
    // }
    //
    // ByteArrayResource resource = new ByteArrayResource(invoiceFile.getData());
    //
    // HttpHeaders headers = new HttpHeaders();
    // String contentType =
    // invoiceFile.getContentType() == null
    // ? MediaType.APPLICATION_PDF_VALUE
    // : invoiceFile.getContentType();
    // headers.set(HttpHeaders.CONTENT_TYPE, contentType);
    // headers.set(
    // HttpHeaders.CONTENT_DISPOSITION,
    // "attachment; filename=\"" + invoiceFile.getFilename() + "\"");
    // if (invoiceFile.getSize() != null) {
    // headers.setContentLength(invoiceFile.getSize());
    // }
    //
    // return ResponseEntity.ok().headers(headers).body(resource);
    // } catch (APIException e) {
    // e.printStackTrace();
    // return ResponseEntity.status(500)
    // .body(ApiResponse.error(e.getErrorDesc().orElse(e.getMessage())));
    // } catch (Exception e) {
    // e.printStackTrace();
    // return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
    // }
    // }

}
