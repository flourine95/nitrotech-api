package com.nitrotech.api.domain.payment.usecase;

import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.exception.PaymentMethodUnsupportedException;
import com.nitrotech.api.domain.order.usecase.GetOrderUseCase;
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

    private final GetOrderUseCase getOrderUseCase;
    private final PaymentProviderResolver paymentProviderResolver;

    public PaymentInitResult execute(Long orderId, Long userId) {
        OrderData order = getOrderUseCase.execute(orderId, userId);

        if (!OrderStatus.PENDING.value().equals(order.status())) {
            throw new BadRequestException(
                    "ORDER_PAYMENT_INITIATION_NOT_ALLOWED",
                    "Payment can only be initiated for pending orders"
            );
        }

        PaymentMethod paymentMethod = PaymentMethod.fromValue(order.paymentMethod());
        if (paymentMethod == PaymentMethod.COD) {
            throw new BadRequestException(
                    "PAYMENT_INITIATION_NOT_REQUIRED",
                    "COD orders do not require payment initiation"
            );
        }
        if (paymentMethod == null) {
            throw new PaymentMethodUnsupportedException(order.paymentMethod());
        }

        PaymentProvider provider = paymentProviderResolver.getProvider(paymentMethod.value());
        return provider.initiatePayment(new PaymentOrderData(
                order.id(),
                order.finalAmount(),
                "Payment for order " + order.orderCode()
        ));
    }
}
