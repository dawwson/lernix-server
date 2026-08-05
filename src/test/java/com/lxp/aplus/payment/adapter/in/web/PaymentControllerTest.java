package com.lxp.aplus.payment.adapter.in.web;

import com.lxp.aplus.common.security.JwtAuthenticationFilter;
import com.lxp.aplus.payment.application.port.in.PaymentUseCase;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentConfirmCommand;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentPrepareCommand;
import com.lxp.aplus.payment.application.port.in.model.result.PaymentPrepareResult;
import com.lxp.aplus.testing.security.WithMockAuthenticatedUser;
import com.lxp.aplus.user.application.port.in.UserQueryUseCase;
import com.lxp.aplus.user.application.port.out.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static com.lxp.aplus.testing.security.WithMockAuthenticatedUser.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockAuthenticatedUser
@DisplayName("PaymentController 테스트")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentUseCase paymentUseCase;

    @MockitoBean
    private UserQueryUseCase userQueryUseCase;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("결제를 준비하면 인증 사용자 ID와 주문 ID를 전달한다")
    void preparePayment_validRequest_passesAuthenticatedUserId() throws Exception {
        given(paymentUseCase.prepare(any(PaymentPrepareCommand.class)))
                .willReturn(new PaymentPrepareResult(
                        "payment-1",
                        "order-1",
                        BigDecimal.valueOf(40_000)
                ));

        mockMvc.perform(post("/api/payments/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"order-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SPM001"))
                .andExpect(jsonPath("$.data.paymentId").value("payment-1"))
                .andExpect(jsonPath("$.data.orderId").value("order-1"))
                .andExpect(jsonPath("$.data.amount").value(40_000));

        ArgumentCaptor<PaymentPrepareCommand> captor = ArgumentCaptor.forClass(PaymentPrepareCommand.class);
        then(paymentUseCase).should().prepare(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new PaymentPrepareCommand(USER_ID, "order-1"));
    }

    @Test
    @DisplayName("주문 ID가 없으면 결제를 준비하지 않는다")
    void preparePayment_missingOrderId_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/payments/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        then(paymentUseCase).should(never()).prepare(any(PaymentPrepareCommand.class));
    }

    @Test
    @DisplayName("결제를 승인하면 인증 사용자 ID와 승인 정보를 전달한다")
    void confirmPayment_validRequest_passesAuthenticatedUserId() throws Exception {
        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": "payment-1",
                                  "orderId": "order-1",
                                  "amount": 40000,
                                  "paymentKey": "payment-key"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SPM002"));

        ArgumentCaptor<PaymentConfirmCommand> captor = ArgumentCaptor.forClass(PaymentConfirmCommand.class);
        then(paymentUseCase).should().confirm(captor.capture());
        assertThat(captor.getValue()).isEqualTo(
                new PaymentConfirmCommand(
                        USER_ID,
                        "payment-1",
                        "order-1",
                        "payment-key",
                        BigDecimal.valueOf(40_000)
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"orderId\":\"order-1\",\"amount\":40000,\"paymentKey\":\"payment-key\"}",
            "{\"paymentId\":\"payment-1\",\"amount\":40000,\"paymentKey\":\"payment-key\"}",
            "{\"paymentId\":\"payment-1\",\"orderId\":\"order-1\",\"paymentKey\":\"payment-key\"}",
            "{\"paymentId\":\"payment-1\",\"orderId\":\"order-1\",\"amount\":40000}"
    })
    @DisplayName("필수 승인 정보가 없으면 결제를 승인하지 않는다")
    void confirmPayment_missingRequiredField_returnsBadRequest(String requestBody) throws Exception {
        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        then(paymentUseCase).should(never()).confirm(any(PaymentConfirmCommand.class));
    }
}
