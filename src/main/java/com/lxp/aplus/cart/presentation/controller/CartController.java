package com.lxp.aplus.cart.presentation.controller;

import com.lxp.aplus.common.result.ResultResponse;
import com.lxp.aplus.common.security.Authenticated;
import com.lxp.aplus.cart.application.command.CartRemoveItemCommand;
import com.lxp.aplus.cart.application.port.in.CartUseCase;
import com.lxp.aplus.cart.application.result.CartAddItemResult;
import com.lxp.aplus.cart.application.result.CartGetItemsResult;
import com.lxp.aplus.cart.application.result.CartRemoveItemResult;
import com.lxp.aplus.cart.presentation.request.CartAddItemRequest;
import com.lxp.aplus.cart.presentation.response.CartAddItemResponse;
import com.lxp.aplus.cart.presentation.response.CartGetItemsResponse;
import com.lxp.aplus.cart.presentation.response.CartRemoveItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.lxp.aplus.common.result.code.CartResultCode.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartUseCase cartUseCase;

    @GetMapping
    public ResponseEntity<ResultResponse<CartGetItemsResponse>> getCartAllItems(
            @Authenticated Long userId
    ) {

        CartGetItemsResult result = cartUseCase.getCartItems(userId);
        CartGetItemsResponse response = CartGetItemsResponse.from(result);

        return ResponseEntity
                .status(CART_GET_ITEMS_SUCCESS.getStatus())
                .body(ResultResponse.of(CART_GET_ITEMS_SUCCESS, response));
    }

    @PostMapping("/items")
    public ResponseEntity<ResultResponse<CartAddItemResponse>> addCartItem(
            @Authenticated Long userId,
            @RequestBody @Valid CartAddItemRequest request
    ) {

        CartAddItemResult result = cartUseCase.addCartItemToCart(request.toCommand(userId));
        CartAddItemResponse response = CartAddItemResponse.from(result);

        return ResponseEntity
                .status(CART_ADD_ITEM_SUCCESS.getStatus())
                .body(ResultResponse.of(CART_ADD_ITEM_SUCCESS, response));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ResultResponse<CartRemoveItemResponse>> removeCartItem(
            @Authenticated Long userId,
            @PathVariable Long cartItemId
    ) {
        CartRemoveItemCommand command = CartRemoveItemCommand.of(userId, cartItemId);
        CartRemoveItemResult result = cartUseCase.removeCartItemFromCart(command);
        CartRemoveItemResponse response = CartRemoveItemResponse.from(result);

        return ResponseEntity
                .status(CART_REMOVE_ITEM_SUCCESS.getStatus())
                .body(ResultResponse.of(CART_REMOVE_ITEM_SUCCESS, response));
    }
}
