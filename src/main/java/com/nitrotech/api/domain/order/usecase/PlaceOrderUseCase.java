package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.address.exception.AddressNotFoundException;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.inventory.exception.InsufficientStockException;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.dto.*;
import com.nitrotech.api.domain.order.exception.CartEmptyException;
import com.nitrotech.api.domain.order.exception.PaymentMethodUnsupportedException;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.payment.PaymentMethod;
import com.nitrotech.api.domain.promotion.dto.ApplyPromotionResult;
import com.nitrotech.api.domain.promotion.repository.PromotionRepository;
import com.nitrotech.api.domain.promotion.usecase.ValidatePromotionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCase {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final InventoryRepository inventoryRepository;
    private final ValidatePromotionUseCase validatePromotionUseCase;
    private final PromotionRepository promotionRepository;

    @Value("${app.shipping.free-threshold:500000}")
    private BigDecimal freeShippingThreshold;

    @Value("${app.shipping.flat-fee:30000}")
    private BigDecimal flatShippingFee;

    @Transactional
    public OrderData execute(CreateOrderCommand command) {
        PaymentMethod paymentMethod = PaymentMethod.fromValue(command.paymentMethod());
        if (paymentMethod == null || !List.of(PaymentMethod.COD, PaymentMethod.SEPAY).contains(paymentMethod)) {
            throw new PaymentMethodUnsupportedException(command.paymentMethod());
        }

        CartData cart = cartRepository.getOrCreateCart(command.userId());
        if (cart.items().isEmpty()) {
            throw new CartEmptyException();
        }

        ShippingAddressSnapshot snapshot = command.shippingAddress();
        if (snapshot == null) {
            if (command.addressId() == null) {
                throw AddressNotFoundException.missing();
            }
            var address = addressRepository.findByIdAndUserId(command.addressId(), command.userId())
                    .orElseThrow(() -> AddressNotFoundException.withId(command.addressId()));

            snapshot = ShippingAddressSnapshot.from(address);
        }

        List<OrderItemData> items = cart.items().stream().map(this::toOrderItem).toList();
        BigDecimal totalAmount = items.stream().map(OrderItemData::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ApplyPromotionResult promotion = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (hasText(command.promotionCode())) {
            promotion = validatePromotionUseCase.execute(command.promotionCode(), command.userId(), totalAmount);
            discountAmount = promotion.discountAmount();
        }
        BigDecimal shippingFee = totalAmount.compareTo(freeShippingThreshold) >= 0 ? BigDecimal.ZERO : flatShippingFee;
        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(discountAmount);

        PlaceOrderData data = new PlaceOrderData(
                command.userId(), snapshot, command.paymentMethod(),
                command.promotionCode(), command.note(),
                totalAmount, discountAmount, shippingFee, finalAmount, items
        );

        OrderData order = orderRepository.place(data);
        if (promotion != null) {
            promotionRepository.recordUsage(
                    promotion.promotionId(),
                    command.userId(),
                    order.id(),
                    promotion.code(),
                    promotion.discountAmount()
            );
        }

        cart.items().forEach(this::deductStock);

        cartRepository.clearCart(command.userId());
        return paymentMethod == PaymentMethod.COD
                ? orderRepository.updateStatus(order.id(), OrderStatus.CONFIRMED.value())
                : order;
    }

    private void deductStock(CartItemData item) {
        if (!inventoryRepository.deductIfEnough(item.variantId(), item.quantity())) {
            throw new InsufficientStockException(item.variant().name(), inventoryRepository.getQuantity(item.variantId()));
        }
    }

    private OrderItemData toOrderItem(CartItemData item) {
        return new OrderItemData(null, item.variantId(), item.variant().name(), item.variant().sku(),
                item.quantity(), item.variant().price(), item.subtotal(), null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
