package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.inventory.exception.InsufficientStockException;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.notification.dto.NotificationEvent;
import com.nitrotech.api.domain.notification.service.NotificationPublisher;
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
    private final NotificationPublisher notificationPublisher;

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
        OrderData result = PaymentMethod.COD.value().equals(data.paymentMethod())
                ? orderRepository.updateStatus(order.id(), OrderStatus.CONFIRMED.value())
                : order;
        notificationPublisher.publish(new NotificationEvent(
                null,
                "NEW_ORDER",
                "Đơn hàng mới " + orderLabel(result),
                "Có đơn hàng mới trị giá " + result.finalAmount() + ".",
                "/dashboard/orders/" + result.id(),
                null,
                null,
                "ORDER_READ_ALL"
        ));
        return result;
    }

    private void deductStock(CartItemData item) {
        int before = inventoryRepository.getQuantity(item.variantId());
        if (!inventoryRepository.deductIfEnough(item.variantId(), item.quantity())) {
            throw new InsufficientStockException(item.variant().name(), inventoryRepository.getQuantity(item.variantId()));
        }
        inventoryRepository.findByVariantId(item.variantId())
                .filter(inventory -> before > inventory.lowStockThreshold() && inventory.lowStock())
                .ifPresent(inventory -> notificationPublisher.publish(new NotificationEvent(
                        null,
                        "LOW_STOCK",
                        "Cảnh báo tồn kho",
                        inventory.variantName() + " chỉ còn " + inventory.quantity() + " sản phẩm.",
                        "/dashboard/products",
                        null,
                        null,
                        "INVENTORY_MANAGE"
                )));
    }

    private String orderLabel(OrderData order) {
        return order.orderCode() == null || order.orderCode().isBlank() ? "#" + order.id() : order.orderCode();
    }
}
