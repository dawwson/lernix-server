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
    private final PaymentOrderQueryPort orderQueryPort;
    private final PaymentEventPublisherPort eventPublisher;

    @Override
    public PaymentPrepareResult prepare(PaymentPrepareCommand command) {
        // 1. 주문 조회
        PayableOrder order = orderQueryPort.getPayableOrder(command.orderId(), command.userId());

        // 2. 주문에 대한 결제 정보 조회 및 생성
        Payment payment = paymentRepository.findByOrderId(order.orderId())
                .orElseGet(() -> Payment.create(order.orderId(), order.userId(), order.amount()));

        // 3. 진행 중인 시도를 반환하거나 새로운 시도 생성
        PaymentAttempt attempt = payment.prepareAttempt();
        paymentRepository.save(payment);

        return PaymentPrepareResult.from(payment, attempt);
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
