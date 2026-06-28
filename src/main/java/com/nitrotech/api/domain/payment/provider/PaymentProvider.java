package com.nitrotech.api.domain.payment.provider;

import com.nitrotech.api.application.payment.request.RawWebhookRequest;
import com.nitrotech.api.domain.payment.dto.PaymentInitResult;
import com.nitrotech.api.domain.payment.dto.PaymentOrderData;
import com.nitrotech.api.domain.payment.dto.VerifiedPaymentWebhook;

public interface PaymentProvider {
    String getProviderName();

    PaymentInitResult initiatePayment(PaymentOrderData order);

    VerifiedPaymentWebhook parseAndVerifyWebhook(RawWebhookRequest rawRequest);
}
