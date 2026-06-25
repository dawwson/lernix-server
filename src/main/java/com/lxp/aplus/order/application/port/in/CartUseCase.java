package com.lxp.aplus.order.application.port.in;

import com.lxp.aplus.order.application.command.CartAddItemCommand;
import com.lxp.aplus.order.application.command.CartRemoveItemCommand;
import com.lxp.aplus.order.application.result.CartAddItemResult;
import com.lxp.aplus.order.application.result.CartGetItemsResult;
import com.lxp.aplus.order.application.result.CartRemoveItemResult;

public interface CartUseCase {

    CartAddItemResult addCartItemToCart(CartAddItemCommand command);

    CartRemoveItemResult removeCartItemFromCart(CartRemoveItemCommand command);

    CartGetItemsResult getCartItems(Long userId);
}
