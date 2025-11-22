package com.capstone_project.elderly_platform.services.externals.payos;

import com.capstone_project.elderly_platform.dtos.request.externals.CreatePaymentLinkRequestBody;
import com.capstone_project.elderly_platform.dtos.request.externals.CreatePaymentSuccess;
import com.capstone_project.elderly_platform.dtos.request.externals.CreatePayoutRequest;
import com.capstone_project.elderly_platform.dtos.request.externals.EstimatePayoutRequest;
import com.capstone_project.elderly_platform.dtos.response.externals.PaymentLinkWithQRCodeResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.batch.PayoutBatchRequest;
import vn.payos.model.v1.payoutsAccount.PayoutAccountInfo;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.invoices.InvoicesInfo;
import vn.payos.model.webhooks.ConfirmWebhookResponse;
import vn.payos.model.webhooks.WebhookData;

import java.util.List;
import java.util.Map;

public interface PayOSService {

    // payment

    PaymentLinkWithQRCodeResponse createPaymentLink(CreatePaymentLinkRequestBody requestBody);
    PaymentLink getOrderStatus(long orderId, CreatePaymentSuccess requestBody);
    WebhookData payosTransferHandler(Object body);
    CreatePaymentLinkResponse createOrderLink(CreatePaymentLinkRequestBody requestBody);
    PaymentLink cancelOrderLink(long orderId);
    ConfirmWebhookResponse confirmWebhook(Map<String, String> requestBody);
    InvoicesInfo getInvoiceInfo(long orderId);
    ResponseEntity<?> downloadInvoice(long orderId, String invoiceId);


    // payout

    PayoutAccountInfo getBalanceInfo();
    List<Payout> getAllPayouts(String referenceId, String approvalState, List<String> category,
                               String fromDate, String toDate, Integer limit, Integer offset);
    Payout getPayoutById(String payoutId);
    Payout createPayout(CreatePayoutRequest payoutRequest);
    Payout createPayoutBatch(PayoutBatchRequest payoutBatchRequest);
    Map<String, Object> getEstimatedFees(EstimatePayoutRequest estimatePayoutRequest);
}
