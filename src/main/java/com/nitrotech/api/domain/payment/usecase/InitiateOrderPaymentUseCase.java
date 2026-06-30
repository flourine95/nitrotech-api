package com.nitrotech.api.domain.payment.usecase;

import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.exception.OrderNotFoundException;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.payment.PaymentMethod;
import com.nitrotech.api.domain.payment.dto.PaymentInitResult;
import com.nitrotech.api.domain.payment.dto.PaymentOrderData;
import com.nitrotech.api.domain.payment.provider.PaymentProvider;
import com.nitrotech.api.domain.payment.provider.PaymentProviderResolver;
import com.nitrotech.api.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InitiateOrderPaymentUseCase {

    private final OrderRepository orderRepository;
    private final PaymentProviderResolver paymentProviderResolver;

    public PaymentInitResult execute(Long orderId, Long userId) {
        return execute(orderId, userId, false);
    }

    public PaymentInitResult execute(Long orderId, Long userId, boolean canReadAll) {
        OrderData order = loadOrder(orderId, userId, canReadAll);

        PaymentMethod paymentMethod = PaymentMethod.fromValue(order.paymentMethod());
        if (paymentMethod == null) {
            throw new BadRequestException("PAYMENT_METHOD_UNSUPPORTED",
                    "Payment method is not supported yet: " + order.paymentMethod());
        }
        if (paymentMethod == PaymentMethod.COD) {
            throw new BadRequestException("PAYMENT_INITIATION_NOT_REQUIRED",
                    "Cash on delivery orders do not require online payment initiation");
        }

        OrderStatus orderStatus = OrderStatus.fromValue(order.status());
        if (orderStatus != OrderStatus.PENDING) {
            throw new BadRequestException("ORDER_PAYMENT_INITIATION_NOT_ALLOWED",
                    "Only pending orders can initiate payment");
        }

        PaymentProvider provider = paymentProviderResolver.getProvider(paymentMethod.value());
        String orderCode = order.orderCode() != null && !order.orderCode().isBlank()
                ? order.orderCode()
                : String.valueOf(order.id());

        return provider.initiatePayment(new PaymentOrderData(
                order.id(),
                order.finalAmount(),
                "Payment for order " + orderCode
        ));
    }

    private OrderData loadOrder(Long orderId, Long userId, boolean canReadAll) {
        if (canReadAll) {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> OrderNotFoundException.withId(orderId));
        }
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> OrderNotFoundException.withId(orderId));
    }
}
