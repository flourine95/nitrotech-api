# Third-party Integration Research

This document reviews third-party integration status for NitroTech API based on:

- current source code in the repository
- [ECOMMERCE-BUSINESS-FLOW.md](./ECOMMERCE-BUSINESS-FLOW.md)
- [MODULE-STATUS-ANALYSIS.md](./MODULE-STATUS-ANALYSIS.md)
- [ECOMMERCE-TEST-CASES.md](./ECOMMERCE-TEST-CASES.md)

Scope:

- Payment integrations
- Shipping integrations

Reading rule for this document:

- `Current source` means behavior or structure already visible in the repository now
- `Recommended development` means proposed follow-up work and is not claimed as implemented

## Executive Summary

From the current backend state:

- payment integration is only partially complete
- shipping integration is more mature than payment
- SePay exists in source as a working webhook-driven payment provider
- VNPAY and MoMo are accepted in order request validation, but do not have complete provider implementations in current source
- GHN and GHTK exist in source as shipping providers with shipment creation and webhook status handling

Main business gaps:

- missing customer-facing payment initiation flow
- missing refund and reconciliation flow
- missing shipping quote before checkout
- missing shipping service-level selection
- missing reverse logistics or return shipment flow

## Payment Integration Research

### Current payment scope in source

#### Current source

The payment area currently includes:

- payment provider abstraction
- provider resolver and registry
- SePay provider implementation
- payment webhook endpoint
- payment webhook verification and parsing
- duplicate transaction protection by provider reference
- amount verification against `order.finalAmount`
- payment transaction persistence
- automatic order confirmation when valid payment is received for a `pending` order

#### Current source limitations

The payment module does not yet show a full customer-facing online payment orchestration. Specifically:

- no public payment-init endpoint is clearly wired into checkout
- no complete VNPAY provider in source
- no complete MoMo provider in source
- no refund integration flow
- no broader reconciliation process

### SePay

#### Current source

SePay is the only visibly implemented payment provider in the current repository.

Observed current behavior:

- `PaymentProvider` defines `initiatePayment(...)` and `parseAndVerifyWebhook(...)`
- `SepayPaymentProvider` implements both methods
- `HandlePaymentWebhookUseCase` processes incoming SePay webhook payloads
- webhook authorization is checked by API key
- order ID is extracted from payment code or payment content
- incoming amount is checked against `order.finalAmount`
- duplicate provider reference is ignored
- valid paid transaction can move order from `pending` to `confirmed`

#### Current source payment data flow

Outbound payment-init concept currently visible in source:

- `orderId`
- `amount`
- `description`

Outbound init result concept currently visible in source:

- `paymentUrl`
- `redirect`

Inbound webhook verification currently uses:

- authorization header
- transaction identifier
- transfer type
- transfer amount
- order code embedded in payment content or code
- raw provider payload

#### Already implemented

- QR URL generation in provider
- webhook auth verification
- webhook payload parsing
- amount comparison
- duplicate transaction detection
- payment transaction logging
- order status confirmation

#### Recommended development

- expose a payment-init API for frontend checkout
- add payment status query capability if frontend needs polling
- define refund flow
- define reconciliation job for missed or delayed webhook cases

### VNPAY

#### Current source

`CreateOrderRequest` validation currently accepts `vnpay`, but no complete VNPAY provider implementation is visible in the source review.

That means:

- request-level acceptance exists
- full provider lifecycle does not yet exist

#### Current business implication

The backend currently accepts a payment method name that is not yet fully backed by payment initiation, callback handling, and settlement logic.

#### Recommended development

If VNPAY is intended to be supported, the backend should define:

1. payment initiation request
2. signed redirect URL creation
3. return callback handling
4. server-side webhook or IPN verification if VNPAY flow requires it
5. duplicate callback protection
6. amount verification
7. payment transaction logging
8. order confirmation only after verified success
9. refund support if business requires it

### MoMo

#### Current source

`CreateOrderRequest` validation currently accepts `momo`, but no complete MoMo provider implementation is visible in the source review.

That means:

- request-level acceptance exists
- full provider lifecycle does not yet exist

#### Current business implication

The backend currently accepts a payment method name that is not yet fully backed by provider-side integration and callback security.

#### Recommended development

If MoMo is intended to be supported, the backend should define:

1. payment initiation request
2. redirect or deep-link payload generation
3. callback handling
4. signature verification
5. duplicate callback protection
6. amount verification
7. payment transaction persistence
8. order confirmation only after verified payment
9. refund support if business requires it

### Payment flow that still needs to exist

#### Recommended development

For non-COD online payments, NitroTech should eventually support this end-to-end flow:

1. customer places order
2. backend creates order in `pending`
3. backend initializes provider payment request
4. backend returns payment URL, QR, token, or redirect package to frontend
5. customer completes payment on provider side
6. provider sends callback or webhook
7. backend verifies provider signature and amount
8. backend prevents duplicate payment transaction creation
9. backend updates payment transaction table
10. backend confirms order if payment is valid
11. backend supports later reconciliation and refund if needed

#### Why this matters

Without this flow, NitroTech currently has order placement plus webhook handling, but not a fully closed online payment journey from the customer perspective.

### Amount verification and duplicate transaction handling

#### Current source

Current payment verification behavior already includes:

- compare webhook amount against `order.finalAmount`
- ignore duplicate provider reference if the same provider transaction was already stored
- confirm only when payment is valid and order is still `pending`

#### Recommended development

Future provider implementations should follow the same baseline rules:

- all success callbacks must be verified against order amount
- all provider transaction IDs must be checked for duplicates
- payment log should be written before or together with order state transition
- invalid, duplicate, or unknown transactions must be traceable in logs

### Refund and reconciliation

#### Current source

No complete refund integration flow is visible in the current source review.

No broader reconciliation module or scheduled matching flow is visible either.

#### Recommended development

Refund flow should eventually define:

- which order states allow refund
- who can initiate refund
- how provider refund API is called
- how refund transaction is recorded
- how order status is updated
- how partial versus full refund is handled

Reconciliation flow should eventually define:

- periodic provider transaction fetch or settlement validation
- mismatch detection
- duplicate handling
- manual review queue
- recovery path for missed webhook callbacks

### Payment security risks

#### Current source protections already visible

- unauthorized SePay webhook is checked by API key
- duplicate provider reference is checked
- amount is verified before confirming order

#### Current source and business risks

- accepting `vnpay` and `momo` in request validation without complete provider support
- lack of standardized signature verification framework across providers
- possible replay attempts if provider-specific nonce or signature policies are not enforced consistently
- no broader reconciliation for missed or delayed webhooks
- no refund approval and control model defined yet

#### Recommended development

- tighten order payment method validation to supported providers only, or finish provider implementations
- centralize provider verification policies
- log webhook correlation IDs and verification outcome
- define replay-protection rules where providers support them
- add reconciliation process for webhook loss or mismatch cases

## Shipping Integration Research

### Current shipping scope in source

#### Current source

The shipping area currently includes:

- shipping provider abstraction
- provider registry
- GHN provider
- GHTK provider
- shipment creation from admin flow
- shipment persistence
- shipment logs
- shipping webhook endpoint
- shipment status mapping from provider payloads
- synchronization from shipment status to order status

#### Current source maturity

Shipping is more mature than payment in the current backend because it already has:

- two actual provider implementations
- outbound shipment creation
- inbound shipment webhooks
- provider-specific mapping logic
- tests around provider and webhook behavior

### GHN

#### Current source

GHN currently exists in source with:

- API client
- address resolver
- carrier address resolver
- shipment provider implementation
- webhook handling via shared shipping webhook use case

#### Current source outbound data

Observed shipment creation data includes:

- receiver name
- receiver phone
- receiver street address
- district and ward mapping to GHN carrier codes
- order items
- quantity
- unit price
- COD amount depending on payment method
- package insurance value
- shipping dimensions and weight defaults

#### Current source inbound data

Webhook processing recognizes fields such as:

- `OrderCode`
- `Status`
- `Type`
- `Warehouse`
- related aliases in payload

These are normalized into NitroTech shipment status and shipment logs.

#### Recommended development

- add shipping quote capability before checkout
- allow service-level selection if GHN supports multiple service paths for NitroTech use case
- define reverse logistics if return shipment is needed

### GHTK

#### Current source

GHTK currently exists in source with:

- API client
- address normalizer
- pickup configuration object
- shipment provider implementation
- webhook handling via shared shipping webhook use case

#### Current source outbound data

Observed shipment creation data includes:

- order ID
- pickup info from configuration
- receiver name and phone
- normalized destination address
- product list
- COD pickup amount depending on payment method
- declared value

#### Current source inbound data

Webhook handling recognizes GHTK-style fields such as:

- `label_id`
- `status_id`
- `reason`

Provider status IDs are mapped into internal shipment statuses such as:

- `ready_to_pick`
- `delivering`
- `delivered`
- `cancel`
- `returning`

#### Recommended development

- add quote-before-checkout capability if GHTK supports it for NitroTech use case
- define selectable shipping services if needed
- define return shipment process if after-sales logistics is in scope

### Shipment creation flow

#### Current source

Shipment creation currently behaves like this:

1. admin requests shipment creation for an order
2. backend checks whether shipment already exists
3. backend checks order status must be `confirmed` or `processing`
4. backend resolves provider by explicit request or default config
5. backend sends shipment creation request to provider
6. backend stores shipment with tracking code, fee, estimated delivery, and initial status
7. backend creates shipment log

#### Current source implications

- shipping is an admin-side operational fulfillment action
- shipment creation is not automatically embedded in checkout
- shipment fee captured at provider side is not yet tied back into checkout pricing logic

### Shipment webhook and status synchronization

#### Current source

Current shipping webhook flow:

1. provider sends status callback
2. backend identifies shipment by provider and tracking code
3. provider-specific status is normalized
4. shipment record is updated
5. shipment log is appended
6. order status may be synchronized

Current order synchronization behavior:

- in-transit shipment can move order from `pending` or `confirmed` to `processing`
- delivered shipment can move order to `delivered`
- cancelled orders are not overridden by shipment sync

#### Current source strength

This is already a strong backend operational pattern because provider events can drive downstream order visibility.

### Quote shipping fee before checkout

#### Current source

No customer-facing shipping quote flow is visible in the current source.

Current order placement logic sets:

- `shippingFee = 0`

#### Recommended development

NitroTech should add a pre-checkout shipping quote capability that can:

1. accept destination address or address codes
2. accept cart items or normalized package information
3. query one or more supported providers
4. return fee, ETA, and available service options
5. persist selected shipping service and fee into order totals

#### Why this matters

Without quote-before-checkout:

- the customer does not see real shipping cost
- final order value may not reflect operational shipping expense
- provider shipment fee is discovered too late in the flow

### Service-level selection

#### Current source

No customer-side service-level selection is visible in source.

Current provider integrations appear to use default service behavior and hardcoded or preselected values.

#### Recommended development

Future shipping flow should define:

- standard shipping
- express shipping
- provider-specific service IDs
- expected delivery windows
- selected service persisted on order and shipment

This is not in current source and should be treated as a future enhancement.

### Reverse logistics and return shipment

#### Current source

No complete reverse logistics or return shipment orchestration is visible in current source.

Although some shipment status enums include return-oriented statuses, there is no end-to-end return shipment business flow such as:

- customer return request
- return approval
- return label creation
- pickup scheduling
- returned stock handling

#### Recommended development

If NitroTech needs after-sales logistics, future design should include:

1. return request lifecycle
2. return shipment creation
3. return tracking
4. inspection outcome
5. refund integration
6. inventory restock or damage handling

### Shipping risks

#### Current source and business risks

- shipping fee not calculated during checkout
- shipment fee discovered later than pricing flow
- no customer-side service selection
- no reverse logistics flow
- quote and actual charged fee may diverge if no quote is persisted into order
- provider address mapping quality can affect shipment creation success
- delayed or missing webhook can affect shipment visibility and order status sync

#### Recommended development

- add quote-before-checkout
- persist selected service and fee on order
- add monitoring for shipment creation failures
- define fallback handling for missing webhooks
- define reverse logistics if return business is in scope

## Provider Comparison Table

| Provider | Trạng thái hiện tại trong source | Dữ liệu cần gửi | Dữ liệu nhận về | Webhook/callback | Rủi ro | Đề xuất phát triển |
|------|-----------------------------------|-----------------|-----------------|------------------|--------|--------------------|
| `SePay` | Current source: đã có provider webhook-driven; có parse webhook, verify auth, compare amount, log transaction, confirm order pending | Current source payment-init concept: `orderId`, `amount`, `description`; webhook uses headers, raw body, transaction content, transaction amount | Current source abstraction returns `paymentUrl`; webhook parse ra `providerRef`, `orderId`, `amount`, `status`, raw provider data | Current source: đã có webhook endpoint và xử lý callback | Chưa có payment-init API hoàn chỉnh cho frontend; chưa có refund; chưa có reconciliation | Recommended development: hoàn thiện payment-init flow, payment status tracking, refund, reconciliation |
| `VNPAY` | Current source: chưa có provider hoàn chỉnh; chỉ được request validation chấp nhận | Recommended development: request ký số, order reference, amount, return URL, client context | Recommended development: redirect URL, callback result, provider transaction reference | Recommended development: callback hoặc IPN verification theo mô hình provider | Cho phép chọn payment method nhưng chưa có luồng vận hành hoàn chỉnh | Recommended development: bổ sung provider đầy đủ hoặc siết validation tạm thời chỉ cho provider đã hỗ trợ |
| `MoMo` | Current source: chưa có provider hoàn chỉnh; chỉ được request validation chấp nhận | Recommended development: order reference, amount, redirect or deep-link params, signature fields | Recommended development: payment URL or app handoff data, callback result, transaction reference | Recommended development: callback và signature verification | Cho phép chọn payment method nhưng chưa có luồng vận hành hoàn chỉnh | Recommended development: bổ sung provider đầy đủ hoặc siết validation tạm thời chỉ cho provider đã hỗ trợ |
| `GHN` | Current source: đã có provider; có client, address resolver, create shipment, webhook status handling | Current source: địa chỉ đích, district and ward mapping, item list, quantity, price, COD amount, insurance value, package size and weight | Current source: tracking code, estimated delivery, fee, provider response | Current source: đã có shipping webhook xử lý trạng thái | Chưa có quote trước checkout; chưa có customer service selection; address mapping có thể lỗi | Recommended development: quote-before-checkout, service selection, persistence of chosen fee and service |
| `GHTK` | Current source: đã có provider; có client, address normalizer, create shipment, webhook status handling | Current source: pickup config, receiver info, normalized address, product list, COD amount, declared value | Current source: tracking code, estimated delivery, fee, provider response | Current source: đã có shipping webhook xử lý trạng thái | Chưa có quote trước checkout; pickup config phụ thuộc cấu hình; chưa có return shipment flow | Recommended development: quote-before-checkout, service selection, return shipment design |

## Current Source vs Recommended Development

### Current source

#### Payment

- SePay provider
- payment webhook endpoint
- webhook auth check
- payment amount comparison
- duplicate transaction protection
- payment transaction logging
- order confirmation from valid payment webhook

#### Shipping

- GHN provider
- GHTK provider
- shipment creation
- shipment persistence and logs
- webhook status handling
- order status synchronization from shipment events

### Recommended development

#### Payment

- customer-facing payment-init endpoint
- VNPAY provider completion
- MoMo provider completion
- refund flow
- reconciliation flow
- stronger standardized security model across providers

#### Shipping

- quote shipping fee before checkout
- service-level selection
- persist selected shipping service and fee into order
- reverse logistics and return shipment flow
- monitoring and fallback handling for webhook delays or failures

## Security and Business Risks

### Payment risks

- payment method validation is ahead of actual provider support
- provider verification is not yet standardized across all future providers
- missed or delayed webhooks may leave order and payment state inconsistent
- refund control flow is not yet defined

### Shipping risks

- shipping fee is not reflected in checkout total
- actual provider fee is discovered later in the process
- no customer-side service selection
- address mapping quality can block shipment creation
- no return shipment design for after-sales operations

### Cross-domain risks

- order total may not match real commercial total because shipping fee is `0`
- integration maturity is uneven across providers
- operational fallback for failed callback flows is still limited

## Priority Recommendation Order

Recommended implementation order based on current business risk and source maturity:

1. complete payment-init flow for the currently supported online payment provider
2. tighten or complete support for `vnpay` and `momo`
3. add shipping quote before checkout and persist real shipping fee in order
4. add service-level selection for shipping
5. add payment reconciliation and refund flow
6. add reverse logistics and return shipment flow
7. add broader security hardening and operational monitoring across all third-party callbacks

## BA and Backend Conclusion

From the current repository state:

- NitroTech already has a meaningful integration backbone for SePay, GHN, and GHTK
- the payment side is still incomplete for a full customer online payment journey
- the shipping side is operationally stronger, but still lacks customer-side pricing and service choice
- the highest-value next step is not adding more providers first, but closing the missing end-to-end business flow around payment initiation and shipping quote

This document describes current source reality and distinguishes it from recommended future development. It does not modify application code or runtime configuration.
