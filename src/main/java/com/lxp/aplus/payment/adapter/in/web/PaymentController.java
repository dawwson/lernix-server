package com.lxp.aplus.payment.adapter.in.web;

import com.lxp.aplus.common.result.ResultResponse;
import com.lxp.aplus.common.security.Authenticated;
import com.lxp.aplus.payment.adapter.in.web.request.PaymentConfirmRequest;
import com.lxp.aplus.payment.adapter.in.web.request.PaymentPrepareRequest;
import com.lxp.aplus.payment.adapter.in.web.response.PaymentPrepareResponse;
import com.lxp.aplus.payment.application.port.in.PaymentUseCase;
import com.lxp.aplus.payment.application.port.in.model.result.PaymentPrepareResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.lxp.aplus.common.result.code.PaymentResultCode.PAYMENT_CONFIRM_SUCCESS;
import static com.lxp.aplus.common.result.code.PaymentResultCode.PAYMENT_PREPARE_SUCCESS;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    @PostMapping("/prepare")
    public ResponseEntity<ResultResponse<PaymentPrepareResponse>> preparePayment(
            @Authenticated Long userId,
            @RequestBody @Valid PaymentPrepareRequest request
    ) {

        PaymentPrepareResult result = paymentUseCase.prepare(request.toCommand(userId));
        PaymentPrepareResponse response = PaymentPrepareResponse.from(result);

        return ResponseEntity
                .status(PAYMENT_PREPARE_SUCCESS.getStatus())
                .body(ResultResponse.of(PAYMENT_PREPARE_SUCCESS, response));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ResultResponse<Void>> confirmPayment(
            @Authenticated Long userId,
            @RequestBody @Valid PaymentConfirmRequest request
    ) {

        paymentUseCase.confirm(request.toCommand(userId));

        return ResponseEntity
                .status(PAYMENT_CONFIRM_SUCCESS.getStatus())
                .body(ResultResponse.from(PAYMENT_CONFIRM_SUCCESS));
    }
}
