package com.lxp.aplus.cart.application.port.in;

import com.lxp.aplus.cart.application.command.CartAddItemCommand;
import com.lxp.aplus.cart.application.command.CartRemoveItemCommand;
import com.lxp.aplus.cart.application.result.CartAddItemResult;
import com.lxp.aplus.cart.application.result.CartGetItemsResult;
import com.lxp.aplus.cart.application.result.CartRemoveItemResult;

public interface CartUseCase {

    CartAddItemResult addCartItemToCart(CartAddItemCommand command);

    CartRemoveItemResult removeCartItemFromCart(CartRemoveItemCommand command);

    CartGetItemsResult getCartItems(Long userId);
}
