package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.externals.CreatePayoutRequest;
import com.capstone_project.elderly_platform.dtos.request.externals.EstimatePayoutRequest;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.services.externals.payos.PayOSService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.batch.PayoutBatchRequest;
import vn.payos.model.v1.payoutsAccount.PayoutAccountInfo;

import java.util.List;
import java.util.Map;

@RequestMapping("/api/v1/payouts")
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payout", description = "Operations related to payout management")
public class PayoutController {

    private final PayOSService payOSService;

    @PostMapping("/create")
    public ResponseEntity<ObjectResponse> create(@RequestBody CreatePayoutRequest body) {
        try {
            Payout response = payOSService.createPayout(body);
            return response != null ? ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Create payout successfully", response)) :
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Create payout failed", null));
        } catch (BadRequestException e) {
            log.error("Error found : {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Create payout failed. " + e.getMessage(), null));
        }
    }

    @PostMapping("/batch/create")
    public ResponseEntity<ObjectResponse> createBatch(@RequestBody PayoutBatchRequest body) {
        try {
            Payout response = payOSService.createPayoutBatch(body);
            return response != null ? ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Create payout batch successfully", response)) :
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Create payout batch failed", null));
        } catch (BadRequestException e) {
            log.error("Error found : {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Create payout batch failed. " + e.getMessage(), null));
        }
    }

    @GetMapping("/{payoutId}")
    public ResponseEntity<ObjectResponse> retrieve(@PathVariable String payoutId) {
        try {
            Payout response = payOSService.getPayoutById(payoutId);
            return response != null ? ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Get payout by id successfully", response)) :
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Get payout by id failed", null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Get payout by id failed. " + e.getMessage(), null));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ObjectResponse> retrieveList(
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) String approvalState,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        try {
            List<Payout> response = payOSService.getAllPayouts(referenceId, approvalState, category, fromDate, toDate, limit, offset);
            return response != null ? ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Get all payout successfully", response)) :
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Get all payout failed", null));
        } catch (BadRequestException e) {
            log.error("Error found : {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Get all payout failed. " + e.getMessage(), null));
        }
    }

    @GetMapping("/balance")
    public ResponseEntity<ObjectResponse> getAccountBalance() {
        try {
            PayoutAccountInfo response = payOSService.getBalanceInfo();
            return response != null ? ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Get balance successfully", response)) :
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Get balance failed", null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Get balance failed. " + e.getMessage(), null));
        }
    }

    @PostMapping("/estimate")
    public ResponseEntity<ObjectResponse> estimate(@RequestBody EstimatePayoutRequest request) {
        try {
            Map<String, Object> response = payOSService.getEstimatedFees(request);
            return response != null ? ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Get estimate bank fee payout successfully", response)) :
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Get estimate bank fee payout failed", null));
        } catch (BadRequestException e) {
            log.error("Error found : {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error found", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", "Get estimate bank fee payout failed. " + e.getMessage(), null));
        }
    }

}
