package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.address.exception.AddressNotFoundException;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.order.dto.*;
import com.nitrotech.api.domain.order.exception.CartEmptyException;
import com.nitrotech.api.domain.order.exception.PaymentMethodUnsupportedException;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.payment.PaymentMethod;
import com.nitrotech.api.domain.promotion.dto.ApplyPromotionResult;
import com.nitrotech.api.domain.promotion.usecase.ValidatePromotionUseCase;
import com.nitrotech.api.domain.shipping.dto.ShippingFeeQuoteRequest;
import com.nitrotech.api.domain.shipping.provider.ShippingProviderResolver;
import com.nitrotech.api.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCase {
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 120;

    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final ValidatePromotionUseCase validatePromotionUseCase;
    private final ShippingProviderResolver shippingProviderResolver;
    private final PlaceOrderTransaction placeOrderTransaction;
    private final OrderRepository orderRepository;

    @Value("${app.shipping.free-threshold:500000}")
    private BigDecimal freeShippingThreshold;

    @Value("${app.shipping.flat-fee:30000}")
    private BigDecimal flatShippingFee;

    @Value("${app.shipping.default-provider:ghtk}")
    private String defaultShippingProvider;

    public OrderData execute(CreateOrderCommand command) {
        String idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey());
        if (idempotencyKey != null) {
            var existing = orderRepository.findByUserIdAndIdempotencyKey(command.userId(), idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

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
        BigDecimal shippingFee = quoteShippingFee(snapshot, items, totalAmount);
        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(discountAmount);

        PlaceOrderData data = new PlaceOrderData(
                command.userId(), snapshot, command.paymentMethod(),
                command.promotionCode(), command.note(),
                totalAmount, discountAmount, shippingFee, finalAmount, items,
                idempotencyKey
        );

        return placeOrderTransaction.execute(data, cart, promotion);
    }

    public BigDecimal quoteShippingFee(Long userId, ShippingAddressSnapshot snapshot) {
        CartData cart = cartRepository.getOrCreateCart(userId);
        if (cart.items().isEmpty()) {
            throw new CartEmptyException();
        }
        List<OrderItemData> items = cart.items().stream().map(this::toOrderItem).toList();
        BigDecimal totalAmount = items.stream().map(OrderItemData::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return quoteShippingFee(snapshot, items, totalAmount);
    }

    private OrderItemData toOrderItem(CartItemData item) {
        return new OrderItemData(null, item.variantId(), item.variant().productId(), item.variant().name(), item.variant().sku(),
                item.quantity(), item.variant().price(), item.subtotal(), null,
                item.variant().weightGrams(), item.variant().lengthCm(), item.variant().widthCm(), item.variant().heightCm());
    }

    private BigDecimal quoteShippingFee(ShippingAddressSnapshot snapshot, List<OrderItemData> items, BigDecimal totalAmount) {
        if (totalAmount.compareTo(freeShippingThreshold) >= 0) {
            return BigDecimal.ZERO;
        }
        try {
            return shippingProviderResolver.getProvider(defaultShippingProvider)
                    .quoteFee(new ShippingFeeQuoteRequest(snapshot, items, totalAmount))
                    .fee();
        } catch (RuntimeException ignored) {
            return flatShippingFee;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeIdempotencyKey(String value) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new BadRequestException("IDEMPOTENCY_KEY_TOO_LONG", "Idempotency-Key must not exceed 120 characters");
        }
        return trimmed;
    }
}
