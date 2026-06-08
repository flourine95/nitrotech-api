package com.nitrotech.api.domain.payment.usecase;

import com.nitrotech.api.application.payment.request.SepayWebhookRequest;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.nitrotech.api.infrastructure.persistence.repository.PaymentTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class HandleSepayWebhookUseCase {

    private final OrderRepository orderRepository;
    private final PaymentTransactionJpaRepository paymentTransactionJpa;

    @Value("${sepay.payment-code-prefix:NT}")
    private String paymentCodePrefix;

    @Transactional
    public void execute(SepayWebhookRequest request) {
        String providerRef = providerRef(request);
        if (providerRef != null && paymentTransactionJpa.findByProviderAndProviderRef("sepay", providerRef).isPresent()) {
            return;
        }

        Long orderId = extractOrderId(request);
        if (orderId == null) {
            return;
        }

        Optional<OrderData> maybeOrder = orderRepository.findById(orderId);
        if (maybeOrder.isEmpty()) {
            return;
        }

        OrderData order = maybeOrder.get();
        boolean paid = isIncoming(request) && amountsMatch(request.transferAmount(), order.finalAmount());
        saveTransaction(request, order.id(), paid ? "paid" : "mismatch", providerRef);

        if (paid && "pending".equals(order.status())) {
            orderRepository.updateStatus(order.id(), "confirmed");
        }
    }

    private boolean isIncoming(SepayWebhookRequest request) {
        return "in".equalsIgnoreCase(request.transferType());
    }

    private boolean amountsMatch(BigDecimal transferAmount, BigDecimal finalAmount) {
        return transferAmount != null && transferAmount.compareTo(finalAmount) == 0;
    }

    private String providerRef(SepayWebhookRequest request) {
        if (request.id() != null) {
            return request.id().toString();
        }
        return request.referenceCode();
    }

    private Long extractOrderId(SepayWebhookRequest request) {
        String raw = request.code() != null && !request.code().isBlank()
                ? request.code()
                : request.content();
        if (raw == null) {
            return null;
        }

        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(paymentCodePrefix) + "(\\d+)\\b", Pattern.CASE_INSENSITIVE)
                .matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        return Long.valueOf(matcher.group(1));
    }

    private void saveTransaction(SepayWebhookRequest request, Long orderId, String status, String providerRef) {
        PaymentTransactionEntity entity = new PaymentTransactionEntity();
        entity.setOrderId(orderId);
        entity.setProvider("sepay");
        entity.setAmount(request.transferAmount() != null ? request.transferAmount() : BigDecimal.ZERO);
        entity.setStatus(status);
        entity.setProviderRef(providerRef);
        entity.setProviderData(providerData(request));
        entity.setPaidAt("paid".equals(status) ? LocalDateTime.now() : null);
        paymentTransactionJpa.save(entity);
    }

    private Map<String, Object> providerData(SepayWebhookRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("gateway", request.gateway());
        data.put("transactionDate", request.transactionDate());
        data.put("accountNumber", request.accountNumber());
        data.put("subAccount", request.subAccount());
        data.put("code", request.code());
        data.put("content", request.content());
        data.put("transferType", request.transferType());
        data.put("description", request.description());
        data.put("accumulated", request.accumulated());
        data.put("referenceCode", request.referenceCode());
        return data;
    }
}
