package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.address.repository.AddressRepository;
import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.order.dto.*;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.promotion.dto.ApplyPromotionResult;
import com.nitrotech.api.domain.promotion.repository.PromotionRepository;
import com.nitrotech.api.domain.promotion.usecase.ValidatePromotionUseCase;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
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
        if (!List.of("cod", "sepay").contains(command.paymentMethod())) {
            throw new DomainException("PAYMENT_METHOD_UNSUPPORTED",
                    "Payment method is not supported yet: " + command.paymentMethod()) {};
        }

        CartData cart = cartRepository.getOrCreateCart(command.userId());
        if (cart.items().isEmpty()) {
            throw new DomainException("CART_EMPTY", "Cart is empty") {};
        }

        // Check tồn kho từng item
        cart.items().forEach(item -> {
            if (!inventoryRepository.hasSufficientStock(item.variantId(), item.quantity())) {
                int available = inventoryRepository.getQuantity(item.variantId());
                throw new DomainException("INSUFFICIENT_STOCK",
                        "Insufficient stock for " + item.variant().name() + ". Available: " + available) {};
            }
        });

        ShippingAddressSnapshot snapshot = command.shippingAddress();
        if (snapshot == null) {
            if (command.addressId() == null) {
                throw new NotFoundException("ADDRESS_NOT_FOUND", "Address not found");
            }
            var address = addressRepository.findByIdAndUserId(command.addressId(), command.userId())
                    .orElseThrow(() -> new NotFoundException("ADDRESS_NOT_FOUND", "Address not found"));

            snapshot = new ShippingAddressSnapshot(
                    address.receiver(), address.phone(),
                    address.province(), address.provinceCode(),
                    address.district(), address.districtCode(),
                    address.ward(), address.wardCode(),
                    address.street()
            );
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

        // Trừ tồn kho
        cart.items().forEach(item ->
                inventoryRepository.adjust(item.variantId(), -item.quantity()));

        cartRepository.clearCart(command.userId());
        return "cod".equals(command.paymentMethod())
                ? orderRepository.updateStatus(order.id(), "confirmed")
                : order;
    }

    private OrderItemData toOrderItem(CartItemData item) {
        return new OrderItemData(null, item.variantId(), item.variant().name(), item.variant().sku(),
                item.quantity(), item.variant().price(), item.subtotal(), null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
