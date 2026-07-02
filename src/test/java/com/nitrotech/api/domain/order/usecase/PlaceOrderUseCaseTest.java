package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.dto.CartSummaryData;
import com.nitrotech.api.domain.cart.dto.CartVariantData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.order.dto.CreateOrderCommand;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.PlaceOrderData;
import com.nitrotech.api.domain.order.dto.ShippingAddressSnapshot;
import com.nitrotech.api.domain.promotion.usecase.ValidatePromotionUseCase;
import com.nitrotech.api.domain.shipping.dto.ShippingFeeQuote;
import com.nitrotech.api.domain.shipping.provider.ShippingProvider;
import com.nitrotech.api.domain.shipping.provider.ShippingProviderResolver;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import com.nitrotech.api.shared.exception.ShippingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PlaceOrderUseCaseTest {

    private CartRepository cartRepository;
    private AddressRepository addressRepository;
    private ValidatePromotionUseCase validatePromotionUseCase;
    private ShippingProviderResolver shippingProviderResolver;
    private ShippingProvider shippingProvider;
    private PlaceOrderTransaction placeOrderTransaction;
    private PlaceOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        addressRepository = mock(AddressRepository.class);
        validatePromotionUseCase = mock(ValidatePromotionUseCase.class);
        shippingProviderResolver = mock(ShippingProviderResolver.class);
        shippingProvider = mock(ShippingProvider.class);
        placeOrderTransaction = mock(PlaceOrderTransaction.class);
        when(shippingProviderResolver.getProvider("ghtk")).thenReturn(shippingProvider);
        when(shippingProvider.quoteFee(any())).thenReturn(new ShippingFeeQuote(new BigDecimal("20000"), BigDecimal.ZERO, true));
        when(placeOrderTransaction.execute(any(), any(), any())).thenAnswer(invocation -> order(invocation.getArgument(0)));
        useCase = new PlaceOrderUseCase(cartRepository, addressRepository, validatePromotionUseCase,
                shippingProviderResolver, placeOrderTransaction);
        ReflectionTestUtils.setField(useCase, "freeShippingThreshold", new BigDecimal("500000"));
        ReflectionTestUtils.setField(useCase, "flatShippingFee", new BigDecimal("30000"));
        ReflectionTestUtils.setField(useCase, "defaultShippingProvider", "ghtk");
    }

    @Test
    void placesOrderFromCartAndClearsCartWhenStockIsAvailable() {
        when(cartRepository.getOrCreateCart(10L)).thenReturn(cart(item(101L, "SKU-101", "RTX 4060", "12000000", 2)));

        OrderData result = useCase.execute(command(addressSnapshot()));

        ArgumentCaptor<PlaceOrderData> captor = ArgumentCaptor.forClass(PlaceOrderData.class);
        verify(placeOrderTransaction).execute(captor.capture(), any(), any());
        PlaceOrderData placed = captor.getValue();
        assertThat(placed.userId()).isEqualTo(10L);
        assertThat(placed.paymentMethod()).isEqualTo("sepay");
        assertThat(placed.totalAmount()).isEqualByComparingTo("24000000");
        assertThat(placed.discountAmount()).isEqualByComparingTo("0");
        assertThat(placed.shippingFee()).isEqualByComparingTo("0");
        assertThat(placed.finalAmount()).isEqualByComparingTo("24000000");
        assertThat(placed.items()).hasSize(1);
        assertThat(placed.items().getFirst().variantId()).isEqualTo(101L);
        assertThat(placed.items().getFirst().quantity()).isEqualTo(2);
        assertThat(placed.shippingAddress().receiver()).isEqualTo("Nguyen Phi Long");

        verify(shippingProvider, never()).quoteFee(any());
        assertThat(result.finalAmount()).isEqualByComparingTo("24000000");
    }

    @Test
    void quotesShippingFeeWhenOrderIsBelowFreeThreshold() {
        when(cartRepository.getOrCreateCart(10L)).thenReturn(cart(item(101L, "SKU-101", "Mouse", "300000", 1)));

        OrderData result = useCase.execute(new CreateOrderCommand(10L, null, addressSnapshot(), "cod", null, null));

        assertThat(result.shippingFee()).isEqualByComparingTo("20000");
        assertThat(result.finalAmount()).isEqualByComparingTo("320000");
        verify(shippingProvider).quoteFee(any());
    }

    @Test
    void acceptsVnpayPaymentMethod() {
        when(cartRepository.getOrCreateCart(10L)).thenReturn(cart(item(101L, "SKU-101", "Keyboard", "800000", 1)));

        OrderData result = useCase.execute(new CreateOrderCommand(10L, null, addressSnapshot(), "vnpay", null, null));

        assertThat(result.paymentMethod()).isEqualTo("vnpay");
    }

    @Test
    void rejectsUnsupportedPaymentMethod() {
        assertThatThrownBy(() -> useCase.execute(new CreateOrderCommand(10L, null, addressSnapshot(), "momo", null, null)))
                .isInstanceOf(DomainException.class)
                .hasMessage("Payment method is not supported yet: momo");

        verify(cartRepository, never()).getOrCreateCart(anyLong());
    }

    @Test
    void loadsSavedAddressWhenSnapshotIsNotProvided() {
        when(cartRepository.getOrCreateCart(10L)).thenReturn(cart(item(101L, "SKU-101", "RTX 4060", "12000000", 1)));
        when(addressRepository.findByIdAndUserId(55L, 10L)).thenReturn(Optional.of(address()));

        useCase.execute(new CreateOrderCommand(10L, 55L, null, "cod", null, "call first"));

        ArgumentCaptor<PlaceOrderData> captor = ArgumentCaptor.forClass(PlaceOrderData.class);
        verify(placeOrderTransaction).execute(captor.capture(), any(), any());
        assertThat(captor.getValue().shippingAddress().receiver()).isEqualTo("Saved Receiver");
        assertThat(captor.getValue().shippingAddress().province()).isEqualTo("Ho Chi Minh");
        assertThat(captor.getValue().note()).isEqualTo("call first");
    }

    @Test
    void rejectsEmptyCart() {
        when(cartRepository.getOrCreateCart(10L)).thenReturn(cart());

        assertThatThrownBy(() -> useCase.execute(command(addressSnapshot())))
                .isInstanceOf(DomainException.class)
                .hasMessage("Cart is empty");

        verify(placeOrderTransaction, never()).execute(any(), any(), any());
    }

    @Test
    void fallsBackToFlatShippingFeeWhenCarrierQuoteFails() {
        when(cartRepository.getOrCreateCart(10L)).thenReturn(cart(item(101L, "SKU-101", "RTX 4060", "12000000", 3)));
        ReflectionTestUtils.setField(useCase, "freeShippingThreshold", new BigDecimal("99999999"));
        when(shippingProvider.quoteFee(any())).thenThrow(new RuntimeException("down"));

        OrderData result = useCase.execute(command(addressSnapshot()));

        assertThat(result.shippingFee()).isEqualByComparingTo("30000");
    }

    @Test
    void rethrowsCarrierConfigErrorsWhenQuotingShippingFee() {
        when(cartRepository.getOrCreateCart(10L)).thenReturn(cart(item(101L, "SKU-101", "Mouse", "300000", 1)));
        when(shippingProvider.quoteFee(any())).thenThrow(
                new ShippingException("GHTK_TOKEN_MISSING", "GHTK token is required when shipment simulation is disabled")
        );

        assertThatThrownBy(() -> useCase.quoteShippingFee(10L, addressSnapshot()))
                .isInstanceOf(ShippingException.class)
                .hasMessage("GHTK token is required when shipment simulation is disabled");
    }

    @Test
    void rejectsOrderWhenSavedAddressCannotBeFound() {
        when(cartRepository.getOrCreateCart(10L)).thenReturn(cart(item(101L, "SKU-101", "RTX 4060", "12000000", 1)));
        when(addressRepository.findByIdAndUserId(55L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new CreateOrderCommand(10L, 55L, null, "cod", null, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Address with ID 55 not found");

        verify(placeOrderTransaction, never()).execute(any(), any(), any());
    }

    private CreateOrderCommand command(ShippingAddressSnapshot snapshot) {
        return new CreateOrderCommand(10L, null, snapshot, "sepay", null, "deliver in office hours");
    }

    private CartData cart(CartItemData... items) {
        return new CartData(1L, 10L, List.of(items), new CartSummaryData(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private CartItemData item(Long variantId, String sku, String name, String price, int quantity) {
        BigDecimal unitPrice = new BigDecimal(price);
        return new CartItemData(
                variantId,
                1L,
                variantId,
                new CartVariantData(variantId, 99L, sku, name, unitPrice, Map.of(), true, null, null,
                        1000, null, null, null, null, null, null, null, null),
                quantity,
                unitPrice.multiply(BigDecimal.valueOf(quantity)),
                Instant.now(),
                Instant.now()
        );
    }

    private ShippingAddressSnapshot addressSnapshot() {
        return new ShippingAddressSnapshot(
                "Nguyen Phi Long",
                "0900000000",
                "Ho Chi Minh",
                "79",
                "Quan 1",
                "760",
                "Ben Nghe",
                "26734",
                "1 Nguyen Hue"
        );
    }

    private AddressData address() {
        return new AddressData(
                55L,
                10L,
                "Saved Receiver",
                "0911111111",
                "Ho Chi Minh",
                "79",
                "Quan 3",
                "770",
                "Vo Thi Sau",
                "27142",
                "2 Pasteur",
                true,
                Instant.now(),
                Instant.now()
        );
    }

    private OrderData order(PlaceOrderData data) {
        return new OrderData(
                777L,
                data.userId(),
                "SO-777",
                data.shippingAddress(),
                "pending",
                data.paymentMethod(),
                data.totalAmount(),
                data.discountAmount(),
                data.shippingFee(),
                data.finalAmount(),
                data.promotionCode(),
                data.note(),
                data.items(),
                Instant.now(),
                Instant.now(),
                null,
                null
        );
    }

}
