package com.nitrotech.api.domain.payment.usecase;

import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.exception.OrderNotFoundException;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.payment.dto.PaymentInitResult;
import com.nitrotech.api.domain.payment.dto.PaymentOrderData;
import com.nitrotech.api.domain.payment.provider.PaymentProviderResolver;
import com.nitrotech.api.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InitiateOrderPaymentUseCase {

    private final OrderRepository orderRepository;
    private final PaymentProviderResolver paymentProviderResolver;

    public PaymentInitResult execute(Long userId, Long orderId) {
        OrderData order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(OrderNotFoundException::new);

        if (!OrderStatus.is(order.status(), OrderStatus.PENDING)) {
            throw new BadRequestException("ORDER_PAYMENT_NOT_PENDING", "Only pending orders can initiate payment");
        }

        return paymentProviderResolver.getProvider(order.paymentMethod())
                .initiatePayment(new PaymentOrderData(order.id(), order.finalAmount(), order.orderCode()));
    }
}
