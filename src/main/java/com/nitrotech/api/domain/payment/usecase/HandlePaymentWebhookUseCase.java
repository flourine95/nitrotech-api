package com.nitrotech.api.domain.payment.usecase;

import com.nitrotech.api.application.payment.request.RawWebhookRequest;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.order.usecase.UpdateOrderStatusUseCase;
import com.nitrotech.api.domain.payment.dto.VerifiedPaymentWebhook;
import com.nitrotech.api.domain.payment.provider.PaymentProvider;
import com.nitrotech.api.domain.payment.provider.PaymentProviderResolver;
import com.nitrotech.api.domain.payment.repository.PaymentTransactionRepository;
import com.nitrotech.api.shared.exception.BadRequestException;
import com.nitrotech.api.shared.exception.NotFoundException;
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
        log.info("Processing webhook callback for provider: {}", providerName);

        try {
            // 1. Resolve provider
            PaymentProvider provider = registry.getProvider(providerName);

            // 2. Adapter parses and verifies signature (throws exception if invalid)
            VerifiedPaymentWebhook verified = provider.parseAndVerifyWebhook(rawRequest);

            // 3. Deduplicate transaction by provider reference
            if (paymentTransactionRepository.existsByProviderAndProviderRef(provider.getProviderName(), verified.externalTransactionId())) {
                log.warn("Duplicate payment webhook detected for provider: {}, ref: {}", provider.getProviderName(), verified.externalTransactionId());
                return Map.of("success", true, "message", "Duplicate transaction ignored");
            }

            // 4. Retrieve order
            OrderData order = orderRepository.findById(verified.orderId())
                    .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND",
                            "Order with ID " + verified.orderId() + " not found"));

            // 5. Verify transaction amount matching
            boolean isPaid = "paid".equals(verified.status()) 
                    && verified.amount() != null 
                    && verified.amount().compareTo(order.finalAmount()) == 0;

            String outcomeStatus = isPaid ? "paid" : "mismatch";

            // 6. Save Payment Transaction Log
            paymentTransactionRepository.save(verified, outcomeStatus);

            // 7. Update order status if paid successfully
            if (isPaid && "pending".equals(order.status())) {
                updateOrderStatusUseCase.execute(order.id(), "confirmed");
            }

            return Map.of("success", isPaid);
        } catch (BadRequestException e) {
            if ("ORDER_ID_NOT_FOUND".equals(e.getCode())) {
                log.warn("Ignored webhook: {}", e.getMessage());
                return Map.of("success", false, "message", "Ignored: " + e.getMessage());
            }
            throw e;
        } catch (NotFoundException e) {
            if ("ORDER_NOT_FOUND".equals(e.getCode())) {
                log.warn("Ignored webhook: {}", e.getMessage());
                return Map.of("success", false, "message", "Ignored: " + e.getMessage());
            }
            throw e;
        }
    }

}
