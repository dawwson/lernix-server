package com.lxp.aplus.integration.payment;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.ErrorCode;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import com.lxp.aplus.payment.adapter.out.persistence.PaymentJpaRepository;
import com.lxp.aplus.payment.application.port.in.PaymentUseCase;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentConfirmCommand;
import com.lxp.aplus.payment.application.port.out.event.PaymentEventPublisherPort;
import com.lxp.aplus.payment.domain.Payment;
import com.lxp.aplus.payment.domain.PaymentAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("결제 승인 동시성 통합 테스트")
class PaymentConfirmConcurrencyIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(40_000);

    @Autowired
    private PaymentUseCase paymentUseCase;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @MockitoBean
    private PaymentEventPublisherPort eventPublisher;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("동일한 승인 요청을 동시에 실행해도 상태 변경과 완료 이벤트는 한 번이다")
    void confirm_sameRequestConcurrently_approvesAndPublishesOnce() throws Exception {
        // Given: 아직 승인되지 않은 Payment와 PENDING Attempt
        PreparedPayment prepared = savePreparedPayment("order-1");
        PaymentConfirmCommand command = command(prepared, "payment-key");

        // When: 완전히 동일한 승인 요청을 두 스레드에서 동시에 실행
        List<ConfirmResult> results = confirmConcurrently(command, command);

        // Then: 첫 요청이 승인하고, 잠금 뒤 실행된 요청은 같은 승인임을 확인해 성공으로 종료
        assertThat(results).allMatch(ConfirmResult::isSuccess);
        assertApproved(prepared, "payment-key");
        // 상태를 실제로 변경한 첫 요청만 완료 이벤트를 발행한다.
        verify(eventPublisher, times(1)).publish(org.mockito.ArgumentMatchers.argThat(
                event -> event.paymentId().equals(prepared.paymentId())));
    }

    @Test
    @DisplayName("동일 Payment에 서로 다른 paymentKey로 동시에 승인하면 하나만 성공한다")
    void confirm_samePaymentWithDifferentKeys_approvesOnce() throws Exception {
        // Given: 두 요청이 함께 사용할 하나의 Payment와 PaymentAttempt
        PreparedPayment prepared = savePreparedPayment("order-1");

        // When: 같은 Payment를 서로 다른 PG 승인 키로 동시에 승인
        List<ConfirmResult> results = confirmConcurrently(
                command(prepared, "payment-key-1"),
                command(prepared, "payment-key-2")
        );

        // Then: Payment 행 잠금을 먼저 획득한 요청만 승인되고, 다른 요청은 승인 정보 충돌
        assertOneSuccessAndOneApprovalConflict(results);
        Payment savedPayment = paymentRepository.findById(prepared.paymentId()).orElseThrow();
        PaymentAttempt approvedAttempt = findAttempt(savedPayment, prepared.attemptId());
        // 스레드 실행 순서는 보장하지 않으므로 어느 키가 승인될지는 특정하지 않는다.
        assertThat(approvedAttempt.getPaymentKey()).isIn("payment-key-1", "payment-key-2");
        verify(eventPublisher, times(1)).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("동일 paymentKey로 서로 다른 Payment를 동시에 승인하면 하나만 성공한다")
    void confirm_differentPaymentsWithSameKey_approvesOnce() throws Exception {
        // Given: 서로 잠금 경합이 없는 별개의 Payment 두 개
        PreparedPayment first = savePreparedPayment("order-1");
        PreparedPayment second = savePreparedPayment("order-2");

        // When: 두 Payment를 동일한 PG 승인 키로 동시에 승인
        List<ConfirmResult> results = confirmConcurrently(
                command(first, "shared-payment-key"),
                command(second, "shared-payment-key")
        );

        // Then: 서로 다른 Payment 행 잠금으로는 막을 수 없지만 payment_key UNIQUE가 하나를 차단
        assertOneSuccessAndOneApprovalConflict(results);
        assertThat(paymentRepository.findAll())
                .filteredOn(payment -> payment.getStatus() == Payment.Status.PAID)
                .hasSize(1);
        verify(eventPublisher, times(1)).publish(org.mockito.ArgumentMatchers.any());
    }

    private PreparedPayment savePreparedPayment(String orderId) {
        Payment payment = Payment.create(orderId, USER_ID, AMOUNT);
        PaymentAttempt attempt = payment.prepareAttempt();
        paymentRepository.saveAndFlush(payment);
        return new PreparedPayment(payment.getId(), attempt.getId(), orderId);
    }

    private List<ConfirmResult> confirmConcurrently(
            PaymentConfirmCommand firstCommand,
            PaymentConfirmCommand secondCommand
    ) throws Exception {
        // readyLatch: 두 작업이 모두 스레드에 배정되어 출발선에 도착할 때까지 기다린다.
        CountDownLatch readyLatch = new CountDownLatch(2); // 2 = 동시 실행할 스레드 개수
        // startLatch: 두 작업이 한쪽의 선행 실행 없이 같은 시점에 confirm을 시작하게 한다.
        CountDownLatch startLatch = new CountDownLatch(1); // 1 =

        Future<ConfirmResult> first = executor.submit(
                () -> confirm(firstCommand, readyLatch, startLatch));
        Future<ConfirmResult> second = executor.submit(
                () -> confirm(secondCommand, readyLatch, startLatch));

        assertThat(readyLatch.await(3, TimeUnit.SECONDS)).isTrue();
        // 두 스레드가 준비된 것을 확인한 뒤 대기 중인 작업을 동시에 출발시킨다.
        startLatch.countDown();

        return List.of(
                first.get(10, TimeUnit.SECONDS),
                second.get(10, TimeUnit.SECONDS)
        );
    }

    private ConfirmResult confirm(
            PaymentConfirmCommand command,
            CountDownLatch readyLatch,
            CountDownLatch startLatch
    ) throws InterruptedException {
        readyLatch.countDown();
        startLatch.await();
        try {
            paymentUseCase.confirm(command);
            return ConfirmResult.success();
        } catch (BusinessException exception) {
            // 예외를 Future 밖으로 던지지 않고 결과로 바꿔 두 동시 요청을 함께 검증한다.
            return ConfirmResult.failure(exception.getErrorCode());
        }
    }

    private void assertApproved(PreparedPayment prepared, String paymentKey) {
        Payment payment = paymentRepository.findById(prepared.paymentId()).orElseThrow();
        PaymentAttempt attempt = findAttempt(payment, prepared.attemptId());
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.PAID);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttempt.Status.APPROVED);
        assertThat(attempt.getPaymentKey()).isEqualTo(paymentKey);
    }

    private void assertOneSuccessAndOneApprovalConflict(List<ConfirmResult> results) {
        assertThat(results).filteredOn(ConfirmResult::isSuccess).hasSize(1);
        assertThat(results)
                .filteredOn(result -> !result.isSuccess())
                .extracting(ConfirmResult::errorCode)
                .containsExactly(PaymentErrorCode.PAYMENT_APPROVAL_CONFLICT);
    }

    private PaymentAttempt findAttempt(Payment payment, String attemptId) {
        return payment.getAttempts().stream()
                .filter(attempt -> attempt.getId().equals(attemptId))
                .findFirst()
                .orElseThrow();
    }

    private PaymentConfirmCommand command(PreparedPayment prepared, String paymentKey) {
        return new PaymentConfirmCommand(
                USER_ID,
                prepared.paymentId(),
                prepared.attemptId(),
                prepared.orderId(),
                paymentKey,
                AMOUNT
        );
    }

    private record PreparedPayment(String paymentId, String attemptId, String orderId) {
    }

    private record ConfirmResult(boolean succeeded, ErrorCode errorCode) {

        static ConfirmResult success() {
            return new ConfirmResult(true, null);
        }

        static ConfirmResult failure(ErrorCode errorCode) {
            return new ConfirmResult(false, errorCode);
        }

        boolean isSuccess() {
            return succeeded;
        }
    }
}
