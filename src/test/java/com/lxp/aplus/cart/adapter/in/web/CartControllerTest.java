package com.lxp.aplus.cart.adapter.in.web;

import com.lxp.aplus.cart.application.port.in.CartUseCase;
import com.lxp.aplus.cart.application.port.in.model.command.CartAddItemCommand;
import com.lxp.aplus.cart.application.port.in.model.command.CartRemoveItemCommand;
import com.lxp.aplus.cart.application.port.in.model.result.CartAddItemResult;
import com.lxp.aplus.cart.application.port.in.model.result.CartGetItemsResult;
import com.lxp.aplus.cart.application.port.in.model.result.CartRemoveItemResult;
import com.lxp.aplus.common.security.JwtAuthenticationFilter;
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

import static com.lxp.aplus.support.WithMockAuthenticatedUser.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockAuthenticatedUser
@DisplayName("CartController 테스트")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartUseCase cartUseCase;

    @MockitoBean
    private UserQueryUseCase userQueryUseCase;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("장바구니를 조회하면 항목과 총액을 반환한다")
    void getCartAllItems_authenticatedUser_returnsCartItems() throws Exception {
        given(cartUseCase.getCartItems(USER_ID))
                .willReturn(CartGetItemsResult.empty(10L));

        mockMvc.perform(get("/api/carts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SO001"))
                .andExpect(jsonPath("$.data.cartId").value(10L))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalAmount").value(0));

        then(cartUseCase).should().getCartItems(USER_ID);
    }

    @Test
    @DisplayName("장바구니 항목을 추가하면 인증 사용자 ID로 command를 생성한다")
    void addCartItem_validRequest_passesAuthenticatedUserId() throws Exception {
        given(cartUseCase.addCartItemToCart(any(CartAddItemCommand.class)))
                .willReturn(new CartAddItemResult(10L, 20L, 30_000));

        mockMvc.perform(post("/api/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SO002"))
                .andExpect(jsonPath("$.data.cartId").value(10L))
                .andExpect(jsonPath("$.data.cartItemId").value(20L))
                .andExpect(jsonPath("$.data.amount").value(30_000));

        ArgumentCaptor<CartAddItemCommand> captor = ArgumentCaptor.forClass(CartAddItemCommand.class);
        then(cartUseCase).should().addCartItemToCart(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new CartAddItemCommand(USER_ID, 100L));
    }

    @Test
    @DisplayName("강좌 ID가 없으면 장바구니 항목을 추가하지 않는다")
    void addCartItem_missingCourseId_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        then(cartUseCase).should(never()).addCartItemToCart(any(CartAddItemCommand.class));
    }

    @Test
    @DisplayName("장바구니 항목을 삭제하면 인증 사용자 ID와 항목 ID를 전달한다")
    void removeCartItem_existingItem_passesAuthenticatedUserId() throws Exception {
        given(cartUseCase.removeCartItemFromCart(any(CartRemoveItemCommand.class)))
                .willReturn(new CartRemoveItemResult(10L, 20L, 0));

        mockMvc.perform(delete("/api/carts/items/{cartItemId}", 20L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SO003"))
                .andExpect(jsonPath("$.data.removedCartItemId").value(20L));

        then(cartUseCase).should().removeCartItemFromCart(
                new CartRemoveItemCommand(USER_ID, 20L)
        );
    }
}
