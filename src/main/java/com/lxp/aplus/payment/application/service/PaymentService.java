package com.lxp.aplus.payment.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import com.lxp.aplus.payment.application.port.in.PaymentUseCase;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentConfirmCommand;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentPrepareCommand;
import com.lxp.aplus.payment.application.port.in.model.result.PaymentPrepareResult;
import com.lxp.aplus.payment.application.port.out.event.PaymentEventPublisherPort;
import com.lxp.aplus.payment.application.port.out.event.model.PaymentCompletedEvent;
import com.lxp.aplus.payment.application.port.out.order.PaymentOrderQueryPort;
import com.lxp.aplus.payment.application.port.out.order.model.PayableOrder;
import com.lxp.aplus.payment.application.port.out.repository.PaymentRepositoryPort;
import com.lxp.aplus.payment.domain.Payment;
import com.lxp.aplus.payment.domain.PaymentAttempt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService implements PaymentUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentIdempotencyService idempotencyService;
    private final PaymentOrderQueryPort orderQueryPort;
    private final PaymentEventPublisherPort eventPublisher;

    @Override
    public PaymentPrepareResult prepare(PaymentPrepareCommand command) {
        // 1. 멱등성 검사: 새로운 요청이면 PROCESSING 레코드를, 완료된 재요청이면 기존 PaymentAttempt ID를 받는다.
        PaymentIdempotencyService.BeginResult idempotency = idempotencyService.begin(command);

        // 1-1. 완료된 재요청은 결제를 다시 만들지 않고 기존 prepare 응답 복구
        if (idempotency.isCompleted()) {
            return restorePrepareResult(idempotency.completedResourceId());
        }

        // --- 2. 새로운 요청에 대해서만 결제 준비 처리 ---
        // 2-1. 주문 조회
        PayableOrder order = orderQueryPort.getPayableOrder(command.orderId(), command.userId());

        // 2-2. 주문에 대한 결제가 이미 존재하면 재사용, 없으면 새로 생성
        Payment payment = paymentRepository.findByOrderId(order.orderId())
                .orElseGet(() -> Payment.create(order.orderId(), order.userId(), order.amount()));

        // 2-3. 결제 시도 생성
        PaymentAttempt attempt = payment.prepareAttempt();
        paymentRepository.save(payment);
        // -----------------------------------

        // 3. 요청 결과 확정
        idempotencyService.complete(idempotency.record(), attempt.getId());

        return PaymentPrepareResult.from(payment, attempt);
    }

    private PaymentPrepareResult restorePrepareResult(String resourceId) {
        // resourceId는 최초 요청에서 응답한 PaymentAttempt ID다.
        Payment payment = paymentRepository.findByAttemptId(resourceId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        return PaymentPrepareResult.from(payment, resourceId);
    }

    @Override
    public void confirm(PaymentConfirmCommand command) {
        Payment payment = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        // 2. 결제 소유자 검증
        payment.validateOwner(command.userId());

        // 3. 결제 대상 주문 검증
        payment.validateOrder(command.orderId());

        // 4. PG사에 최종 승인 요청
        // TODO: 추후 구현. 성공했다고 가정함

        // 5. 도메인 불변성 검증 -> 상태 변경
        payment.approve(command.paymentAttemptId(), command.paymentKey(), command.amount());

        // 6. Payment 저장
        paymentRepository.save(payment);

        eventPublisher.publish(
                new PaymentCompletedEvent(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getUserId(),
                    payment.getAmount()
                )
        );
    }

}
