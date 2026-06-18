package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.dto.CartSummaryData;
import com.nitrotech.api.domain.cart.dto.CartVariantData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.order.dto.CreateOrderCommand;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.PlaceOrderData;
import com.nitrotech.api.domain.order.dto.ShippingAddressSnapshot;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PlaceOrderUseCaseTest {

    private OrderRepository orderRepository;
    private CartRepository cartRepository;
    private AddressRepository addressRepository;
    private InventoryRepository inventoryRepository;
    private PlaceOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        cartRepository = mock(CartRepository.class);
        addressRepository = mock(AddressRepository.class);
        inventoryRepository = mock(InventoryRepository.class);
        useCase = new PlaceOrderUseCase(orderRepository, cartRepository, addressRepository, inventoryRepository);
    }

    @Test
    void placesOrderFromCartAndClearsCartWhenStockIsAvailable() {
        when(cartRepository.getOrCreateCart(10L)).thenReturn(cart(item(101L, "SKU-101", "RTX 4060", "12000000", 2)));
        when(inventoryRepository.hasSufficientStock(101L, 2)).thenReturn(true);
        when(orderRepository.place(any())).thenAnswer(invocation -> order(invocation.getArgument(0)));

        OrderData result = useCase.execute(command(addressSnapshot()));

        ArgumentCaptor<PlaceOrderData> captor = ArgumentCaptor.forClass(PlaceOrderData.class);
        verify(orderRepository).place(captor.capture());
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

        verify(inventoryRepository).adjust(101L, -2);
        verify(cartRepository).clearCart(10L);
        assertThat(result.finalAmount()).isEqualByComparingTo("24000000");
    }

    @Test
    void loadsSavedAddressWhenSnapshotIsNotProvided() {
        when(cartRepository.getOrCreateCart(10L)).thenReturn(cart(item(101L, "SKU-101", "RTX 4060", "12000000", 1)));
        when(inventoryRepository.hasSufficientStock(101L, 1)).thenReturn(true);
        when(addressRepository.findByIdAndUserId(55L, 10L)).thenReturn(Optional.of(address()));
        when(orderRepository.place(any())).thenAnswer(invocation -> order(invocation.getArgument(0)));

        useCase.execute(new CreateOrderCommand(10L, 55L, null, "cod", null, "call first"));

        ArgumentCaptor<PlaceOrderData> captor = ArgumentCaptor.forClass(PlaceOrderData.class);
        verify(orderRepository).place(captor.capture());
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

        verify(orderRepository, never()).place(any());
        verify(inventoryRepository, never()).adjust(anyLong(), anyInt());
        verify(cartRepository, never()).clearCart(anyLong());
    }

    @Test
    void rejectsOrderWhenStockIsInsufficient() {
        when(cartRepository.getOrCreateCart(10L)).thenReturn(cart(item(101L, "SKU-101", "RTX 4060", "12000000", 3)));
        when(inventoryRepository.hasSufficientStock(101L, 3)).thenReturn(false);
        when(inventoryRepository.getQuantity(101L)).thenReturn(1);

        assertThatThrownBy(() -> useCase.execute(command(addressSnapshot())))
                .isInstanceOf(DomainException.class)
                .hasMessage("Insufficient stock for RTX 4060. Available: 1");

        verify(orderRepository, never()).place(any());
        verify(inventoryRepository, never()).adjust(anyLong(), anyInt());
        verify(cartRepository, never()).clearCart(anyLong());
    }

    @Test
    void rejectsOrderWhenSavedAddressCannotBeFound() {
        when(cartRepository.getOrCreateCart(10L)).thenReturn(cart(item(101L, "SKU-101", "RTX 4060", "12000000", 1)));
        when(inventoryRepository.hasSufficientStock(101L, 1)).thenReturn(true);
        when(addressRepository.findByIdAndUserId(55L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new CreateOrderCommand(10L, 55L, null, "cod", null, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Address not found");

        verify(orderRepository, never()).place(any());
        verify(inventoryRepository, never()).adjust(anyLong(), anyInt());
        verify(cartRepository, never()).clearCart(anyLong());
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
                new CartVariantData(variantId, 99L, sku, name, unitPrice, Map.of(), true, null, null, null, null, null, null, null),
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
                Instant.now()
        );
    }
}
