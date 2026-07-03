package com.lxp.aplus.common.result.code;

import com.lxp.aplus.common.result.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderResultCode implements ResultCode {

    ORDER_CREATE_SUCCESS(HttpStatus.CREATED, "SO001", "주문이 생성되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
