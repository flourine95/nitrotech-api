package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.inventory.exception.InsufficientStockException;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.PlaceOrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.payment.PaymentMethod;
import com.nitrotech.api.domain.promotion.dto.ApplyPromotionResult;
import com.nitrotech.api.domain.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceOrderTransaction {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final CartRepository cartRepository;
    private final PromotionRepository promotionRepository;

    @Transactional
    public OrderData execute(PlaceOrderData data, CartData cart, ApplyPromotionResult promotion) {
        OrderData order = orderRepository.place(data);
        if (promotion != null) {
            promotionRepository.recordUsage(
                    promotion.promotionId(),
                    data.userId(),
                    order.id(),
                    promotion.code(),
                    promotion.discountAmount()
            );
        }

        cart.items().forEach(this::deductStock);
        cartRepository.clearCart(data.userId());
        return PaymentMethod.COD.value().equals(data.paymentMethod())
                ? orderRepository.updateStatus(order.id(), OrderStatus.CONFIRMED.value())
                : order;
    }

    private void deductStock(CartItemData item) {
        if (!inventoryRepository.deductIfEnough(item.variantId(), item.quantity())) {
            throw new InsufficientStockException(item.variant().name(), inventoryRepository.getQuantity(item.variantId()));
        }
    }
}
