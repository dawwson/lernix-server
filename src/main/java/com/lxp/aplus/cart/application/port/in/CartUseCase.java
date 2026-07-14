package com.lxp.aplus.cart.application.port.in;

import com.lxp.aplus.cart.application.port.in.model.command.CartAddItemCommand;
import com.lxp.aplus.cart.application.port.in.model.command.CartRemoveItemCommand;
import com.lxp.aplus.cart.application.port.in.model.result.CartAddItemResult;
import com.lxp.aplus.cart.application.port.in.model.result.CartGetItemsResult;
import com.lxp.aplus.cart.application.port.in.model.result.CartRemoveItemResult;

public interface CartUseCase {

    CartAddItemResult addCartItemToCart(CartAddItemCommand command);

    CartRemoveItemResult removeCartItemFromCart(CartRemoveItemCommand command);

    CartGetItemsResult getCartItems(Long userId);
}
