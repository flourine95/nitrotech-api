package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.address.repository.AddressRepository;
import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
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

    public PlaceOrderUseCase(OrderRepository orderRepository, CartRepository cartRepository,
                              AddressRepository addressRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
    }

    @Transactional
    public OrderData execute(CreateOrderCommand command) {
        // Lấy cart
        CartData cart = cartRepository.getOrCreateCart(command.userId());
        if (cart.items().isEmpty()) {
            throw new DomainException("CART_EMPTY", "Cart is empty") {};
        }

        // Lấy địa chỉ giao hàng
        var address = addressRepository.findByIdAndUserId(command.addressId(), command.userId())
                .orElseThrow(() -> new NotFoundException("ADDRESS_NOT_FOUND", "Address not found"));

        ShippingAddressSnapshot snapshot = new ShippingAddressSnapshot(
                address.receiver(), address.phone(),
                address.province(), address.provinceCode(),
                address.district(), address.districtCode(),
                address.ward(), address.wardCode(),
                address.street()
        );

        // Tính toán
        List<OrderItemData> items = cart.items().stream().map(this::toOrderItem).toList();
        BigDecimal totalAmount = items.stream().map(OrderItemData::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = BigDecimal.ZERO; // TODO: tích hợp GHN/GHTK
        BigDecimal discountAmount = BigDecimal.ZERO; // TODO: tích hợp promotion
        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(discountAmount);

        PlaceOrderData data = new PlaceOrderData(
                command.userId(), snapshot, command.paymentMethod(),
                command.promotionCode(), command.note(),
                totalAmount, discountAmount, shippingFee, finalAmount, items
        );

        OrderData order = orderRepository.place(data);

        // Clear cart sau khi đặt hàng thành công
        cartRepository.clearCart(command.userId());

        return order;
    }

    private OrderItemData toOrderItem(CartItemData item) {
        return new OrderItemData(
                null, item.variantId(), item.variantName(), item.variantSku(),
                item.quantity(), item.variantPrice(), item.subtotal()
        );
    }
}
