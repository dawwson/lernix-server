package com.lxp.aplus.common.error.code;

import com.lxp.aplus.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    ORDER_INVALID_STATUS(HttpStatus.CONFLICT, "EO001", "해당 주문은 이미 처리 중이거나 완료되어 요청 작업을 수행할 수 없습니다."),
    ORDER_ALREADY_PAID(HttpStatus.CONFLICT, "EO002", "이미 해당 주문에 대해 승인된 결제가 존재합니다."),
    ORDER_AMOUNT_MISMATCH(HttpStatus.CONFLICT, "EO003", "주문 총액과 실제 승인된 결제 금액이 일치하지 않습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "EO004", "주문 정보를 찾을 수 없습니다."),
    ORDER_PAYMENT_CONFLICT(HttpStatus.CONFLICT, "EO005", "이미 다른 결제로 완료된 주문입니다."),
    ORDER_INVALID_ITEM(HttpStatus.BAD_REQUEST, "EO006", "유효하지 않은 주문 항목입니다."),
    ORDER_ITEM_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "EO007", "이미 다른 주문에 포함된 주문 항목입니다."),
    ORDER_INVALID_USER(HttpStatus.BAD_REQUEST, "EO008", "유효하지 않은 주문 사용자입니다."),
    ORDER_INVALID_PAYMENT_INFO(HttpStatus.BAD_REQUEST, "EO009", "유효하지 않은 결제 완료 정보입니다."),
    ORDER_CANCEL_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "EO010", "주문 취소 사유는 필수입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
