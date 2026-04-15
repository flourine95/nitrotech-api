package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.address.repository.AddressRepository;
import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.order.dto.*;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PlaceOrderUseCase {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final InventoryRepository inventoryRepository;

    public PlaceOrderUseCase(OrderRepository orderRepository, CartRepository cartRepository,
                              AddressRepository addressRepository,
                              InventoryRepository inventoryRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public OrderData execute(CreateOrderCommand command) {
        CartData cart = cartRepository.getOrCreateCart(command.userId());
        if (cart.items().isEmpty()) {
            throw new DomainException("CART_EMPTY", "Cart is empty") {};
        }

        // Check tồn kho từng item
        cart.items().forEach(item -> {
            if (!inventoryRepository.hasSufficientStock(item.variantId(), item.quantity())) {
                int available = inventoryRepository.getQuantity(item.variantId());
                throw new DomainException("INSUFFICIENT_STOCK",
                        "Insufficient stock for " + item.variantName() + ". Available: " + available) {};
            }
        });

        var address = addressRepository.findByIdAndUserId(command.addressId(), command.userId())
                .orElseThrow(() -> new NotFoundException("ADDRESS_NOT_FOUND", "Address not found"));

        ShippingAddressSnapshot snapshot = new ShippingAddressSnapshot(
                address.receiver(), address.phone(),
                address.province(), address.provinceCode(),
                address.district(), address.districtCode(),
                address.ward(), address.wardCode(),
                address.street()
        );

        List<OrderItemData> items = cart.items().stream().map(this::toOrderItem).toList();
        BigDecimal totalAmount = items.stream().map(OrderItemData::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(discountAmount);

        PlaceOrderData data = new PlaceOrderData(
                command.userId(), snapshot, command.paymentMethod(),
                command.promotionCode(), command.note(),
                totalAmount, discountAmount, shippingFee, finalAmount, items
        );

        OrderData order = orderRepository.place(data);

        // Trừ tồn kho
        cart.items().forEach(item ->
                inventoryRepository.adjust(item.variantId(), -item.quantity()));

        cartRepository.clearCart(command.userId());
        return order;
    }

    private OrderItemData toOrderItem(CartItemData item) {
        return new OrderItemData(null, item.variantId(), item.variantName(), item.variantSku(),
                item.quantity(), item.variantPrice(), item.subtotal());
    }
}
