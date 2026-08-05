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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        // 1. 결제 대상 주문 조회
        PayableOrder order = orderQueryPort.getPayableOrder(command.orderId(), command.userId());

        // 2. 이전 결제 시도에 따른 재시도 가능 여부 확인
        List<Payment> previousPayments = paymentRepository.findAllByOrderId(order.orderId());

        boolean hasPaymentBlockingNewAttempt = previousPayments.stream()
                .anyMatch(Payment::blocksNewAttempt);
        if (hasPaymentBlockingNewAttempt) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_RETRY_NOT_ALLOWED);
        }

        Payment pendingPayment = previousPayments.stream()
                .filter(Payment::isPending)
                .findFirst()
                .orElse(null);
        if (pendingPayment != null) {
            return PaymentPrepareResult.from(pendingPayment);
        }

        // 3. Payment 생성
        Payment payment = Payment.create(
                order.orderId(),
                order.userId(),
                order.amount()
        );
        paymentRepository.save(payment);

        // 4. 결과 반환
        return PaymentPrepareResult.from(payment);
    }

    @Override
    public void confirm(PaymentConfirmCommand command) {
        // 1. Payment 조회
        Payment payment = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // 2. 결제 소유자 검증
        payment.validateOwner(command.userId());

        // 3. 결제 대상 주문 검증
        payment.validateOrder(command.orderId());

        // 4. PG사에 최종 승인 요청
        // TODO: 추후 구현. 성공했다고 가정함

        // 5. 도메인 불변성 검증 -> 상태 변경
        payment.approve(command.paymentKey(), command.amount());

        // 6. Payment 저장
        paymentRepository.save(payment);

        eventPublisher.publish(
                new PaymentCompletedEvent(
                    payment.getPaymentId(),
                    payment.getOrderId(),
                    payment.getUserId(),
                    payment.getAmount()
                )
        );
    }

}
