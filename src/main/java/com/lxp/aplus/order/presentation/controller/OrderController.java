package com.lxp.aplus.order.presentation.controller;

import com.lxp.aplus.common.result.ResultResponse;
import com.lxp.aplus.common.security.Authenticated;
import com.lxp.aplus.order.application.port.in.OrderUseCase;
import com.lxp.aplus.order.application.result.OrderCreateResult;
import com.lxp.aplus.order.presentation.request.OrderCreateRequest;
import com.lxp.aplus.order.presentation.response.OrderCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.lxp.aplus.common.result.code.OrderResultCode.ORDER_CREATE_SUCCESS;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderUseCase orderUseCase;

    @PostMapping
    public ResponseEntity<ResultResponse<OrderCreateResponse>> createOrder(
            @Authenticated Long userId,
            @RequestBody @Valid OrderCreateRequest request
    ) {
        OrderCreateResult result = orderUseCase.createOrder(request.toCommand(userId));
        OrderCreateResponse response = OrderCreateResponse.from(result);

        return ResponseEntity
                .status(ORDER_CREATE_SUCCESS.getStatus())
                .body(ResultResponse.of(ORDER_CREATE_SUCCESS, response));
    }
}
