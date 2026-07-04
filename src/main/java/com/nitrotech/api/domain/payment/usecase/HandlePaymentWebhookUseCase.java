package com.nitrotech.api.domain.payment.usecase;

import com.nitrotech.api.application.payment.request.RawWebhookRequest;
import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.exception.OrderNotFoundException;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.order.usecase.UpdateOrderStatusUseCase;
import com.nitrotech.api.domain.payment.dto.VerifiedPaymentWebhook;
import com.nitrotech.api.domain.payment.provider.PaymentProvider;
import com.nitrotech.api.domain.payment.provider.PaymentProviderResolver;
import com.nitrotech.api.domain.payment.repository.PaymentTransactionRepository;
import com.nitrotech.api.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandlePaymentWebhookUseCase {

    private final PaymentProviderResolver registry;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @Transactional
    public Map<String, Object> execute(String providerName, RawWebhookRequest rawRequest) {
        try {
            PaymentProvider provider = registry.getProvider(providerName);

            VerifiedPaymentWebhook verified = provider.parseAndVerifyWebhook(rawRequest);

            if (paymentTransactionRepository.existsByProviderAndProviderRef(provider.getProviderName(), verified.externalTransactionId())) {
                return Map.of("success", true, "message", "Duplicate transaction ignored");
            }

            OrderData order = orderRepository.findById(verified.orderId())
                    .orElseThrow(() -> OrderNotFoundException.withId(verified.orderId()));

            boolean isPaid = "paid".equals(verified.status())
                    && verified.amount() != null
                    && verified.amount().compareTo(order.finalAmount()) == 0;

            String outcomeStatus = isPaid ? "paid" : "mismatch";

            paymentTransactionRepository.save(verified, outcomeStatus);

            if (isPaid && OrderStatus.PENDING.value().equals(order.status())) {
                updateOrderStatusUseCase.execute(order.id(), OrderStatus.CONFIRMED.value());
            }

            return Map.of("success", isPaid);
        } catch (BadRequestException e) {
            if ("ORDER_ID_NOT_FOUND".equals(e.getCode())) {
                return Map.of("success", false, "message", "Ignored: " + e.getMessage());
            }
            throw e;
        } catch (OrderNotFoundException e) {
            if ("ORDER_NOT_FOUND".equals(e.getCode())) {
                return Map.of("success", false, "message", "Ignored: " + e.getMessage());
            }
            throw e;
        }
    }

}
