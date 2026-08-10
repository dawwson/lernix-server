package com.lxp.aplus.integration.payment;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import com.lxp.aplus.payment.adapter.out.persistence.IdempotencyRecordJpaRepository;
import com.lxp.aplus.payment.adapter.out.persistence.PaymentJpaRepository;
import com.lxp.aplus.payment.application.port.in.PaymentUseCase;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentPrepareCommand;
import com.lxp.aplus.payment.application.port.in.model.result.PaymentPrepareResult;
import com.lxp.aplus.payment.application.port.out.order.PaymentOrderQueryPort;
import com.lxp.aplus.payment.application.port.out.order.model.PayableOrder;
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
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("결제 준비 동시성 통합 테스트")
class PaymentPrepareConcurrencyIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final String ORDER_ID = "order-1";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(40_000);

    @Autowired
    private PaymentUseCase paymentUseCase;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private IdempotencyRecordJpaRepository idempotencyRecordRepository;

    @MockitoBean
    private PaymentOrderQueryPort orderQueryPort;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        idempotencyRecordRepository.deleteAll();
        paymentRepository.deleteAll();
        executor = Executors.newFixedThreadPool(2);
        given(orderQueryPort.getPayableOrder(ORDER_ID, USER_ID))
                .willReturn(new PayableOrder(ORDER_ID, USER_ID, AMOUNT));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("기존 Payment에 서로 다른 키로 동시에 요청하면 새 Attempt를 하나만 생성한다")
    void prepare_existingPaymentWithDifferentKeys_createsSingleAttempt() throws Exception {
        Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT);
        PaymentAttempt failedAttempt = payment.prepareAttempt();
        payment.fail(failedAttempt);
        paymentRepository.saveAndFlush(payment);

        List<Outcome> outcomes = executeConcurrently(
                command("idempotency-key-1"),
                command("idempotency-key-2")
        );

        assertSingleSuccess(outcomes);
        assertThat(singleFailure(outcomes).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_RETRY_NOT_ALLOWED);

        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(savedPayment.getAttempts())
                .extracting(PaymentAttempt::getStatus)
                .containsExactlyInAnyOrder(PaymentAttempt.Status.FAILED, PaymentAttempt.Status.PENDING);
    }

    @Test
    @DisplayName("Payment가 없을 때 서로 다른 키로 동시에 요청해도 주문당 Payment는 하나만 생성된다")
    void prepare_missingPaymentWithDifferentKeys_createsSinglePayment() throws Exception {
        List<Outcome> outcomes = executeConcurrently(
                command("idempotency-key-1"),
                command("idempotency-key-2")
        );

        assertSingleSuccess(outcomes);
        assertThat(singleFailure(outcomes).getErrorCode())
                .isIn(PaymentErrorCode.PAYMENT_PREPARE_CONFLICT, PaymentErrorCode.PAYMENT_RETRY_NOT_ALLOWED);
        assertThat(paymentRepository.count()).isEqualTo(1);

        Payment payment = paymentRepository.findAll().get(0);
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(savedPayment.getAttempts()).hasSize(1);
        assertThat(idempotencyRecordRepository.count()).isEqualTo(1);
    }

    private List<Outcome> executeConcurrently(
            PaymentPrepareCommand firstCommand,
            PaymentPrepareCommand secondCommand
    ) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Outcome> first = executor.submit(() -> execute(firstCommand, ready, start));
        Future<Outcome> second = executor.submit(() -> execute(secondCommand, ready, start));

        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        return List.of(
                first.get(10, TimeUnit.SECONDS),
                second.get(10, TimeUnit.SECONDS)
        );
    }

    private Outcome execute(
            PaymentPrepareCommand command,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            return Outcome.success(paymentUseCase.prepare(command));
        } catch (BusinessException exception) {
            return Outcome.failure(exception);
        }
    }

    private void assertSingleSuccess(List<Outcome> outcomes) {
        assertThat(outcomes).filteredOn(Outcome::isSuccess).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.isSuccess()).hasSize(1);
    }

    private BusinessException singleFailure(List<Outcome> outcomes) {
        return outcomes.stream()
                .map(Outcome::exception)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow();
    }

    private PaymentPrepareCommand command(String idempotencyKey) {
        return new PaymentPrepareCommand(USER_ID, ORDER_ID, idempotencyKey);
    }

    private record Outcome(PaymentPrepareResult result, BusinessException exception) {

        static Outcome success(PaymentPrepareResult result) {
            return new Outcome(result, null);
        }

        static Outcome failure(BusinessException exception) {
            return new Outcome(null, exception);
        }

        boolean isSuccess() {
            return result != null;
        }
    }
}
