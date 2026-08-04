package com.lxp.aplus.order.adapter.in.web;

import com.lxp.aplus.common.security.JwtAuthenticationFilter;
import com.lxp.aplus.order.application.port.in.OrderUseCase;
import com.lxp.aplus.order.application.port.in.model.command.OrderCreateCommand;
import com.lxp.aplus.order.application.port.in.model.result.OrderCreateResult;
import com.lxp.aplus.support.WithMockAuthenticatedUser;
import com.lxp.aplus.user.application.port.in.UserQueryUseCase;
import com.lxp.aplus.user.application.port.out.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static com.lxp.aplus.support.WithMockAuthenticatedUser.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockAuthenticatedUser
@DisplayName("OrderController 테스트")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderUseCase orderUseCase;

    @MockitoBean
    private UserQueryUseCase userQueryUseCase;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("주문을 생성하면 인증 사용자 ID와 강좌 목록을 전달한다")
    void createOrder_validRequest_passesAuthenticatedUserId() throws Exception {
        given(orderUseCase.createOrder(any(OrderCreateCommand.class)))
                .willReturn(new OrderCreateResult("order-1", BigDecimal.valueOf(40_000)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseIds\":[10,20]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SO001"))
                .andExpect(jsonPath("$.data.orderId").value("order-1"))
                .andExpect(jsonPath("$.data.amount").value(40_000));

        ArgumentCaptor<OrderCreateCommand> captor = ArgumentCaptor.forClass(OrderCreateCommand.class);
        then(orderUseCase).should().createOrder(captor.capture());
        assertThat(captor.getValue()).isEqualTo(
                new OrderCreateCommand(USER_ID, List.of(10L, 20L))
        );
    }

    @Test
    @DisplayName("강좌 목록이 없으면 주문을 생성하지 않는다")
    void createOrder_missingCourseIds_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        then(orderUseCase).should(never()).createOrder(any(OrderCreateCommand.class));
    }

    @Test
    @DisplayName("강좌 목록이 비어 있으면 주문을 생성하지 않는다")
    void createOrder_emptyCourseIds_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseIds\":[]}"))
                .andExpect(status().isBadRequest());

        then(orderUseCase).should(never()).createOrder(any(OrderCreateCommand.class));
    }
}
