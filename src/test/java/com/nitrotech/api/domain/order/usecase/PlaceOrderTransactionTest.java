package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.dto.CartSummaryData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.PlaceOrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.promotion.repository.PromotionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlaceOrderTransactionTest {

    @Test
    void deductsStockClearsCartAndConfirmsCodOrder() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        InventoryRepository inventoryRepository = mock(InventoryRepository.class);
        CartRepository cartRepository = mock(CartRepository.class);
        PromotionRepository promotionRepository = mock(PromotionRepository.class);
        PlaceOrderTransaction transaction = new PlaceOrderTransaction(
                orderRepository, inventoryRepository, cartRepository, promotionRepository);

        PlaceOrderData data = new PlaceOrderData(
                10L, null, "cod", null, null,
                new BigDecimal("300000"), BigDecimal.ZERO, new BigDecimal("20000"), new BigDecimal("320000"),
                List.of(new com.nitrotech.api.domain.order.dto.OrderItemData(
                        null, 101L, 201L, "Mouse", "SKU-101", 2,
                        new BigDecimal("150000"), new BigDecimal("300000"), null,
                        500, null, null, null
                )),
                null
        );
        CartData cart = new CartData(1L, 10L, List.of(cartItem()), new CartSummaryData(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(orderRepository.place(data)).thenReturn(order("pending"));
        when(inventoryRepository.deductIfEnough(101L, 2)).thenReturn(true);
        when(orderRepository.updateStatus(777L, "confirmed")).thenReturn(order("confirmed"));

        OrderData result = transaction.execute(data, cart, null);

        assertThat(result.status()).isEqualTo("confirmed");
        verify(inventoryRepository).deductIfEnough(101L, 2);
        verify(cartRepository).clearCart(10L);
    }

    private CartItemData cartItem() {
        return new CartItemData(1L, 1L, 101L, null, 2, new BigDecimal("300000"), Instant.now(), Instant.now());
    }

    private OrderData order(String status) {
        return new OrderData(
                777L, 10L, "SO-777", null, status, "cod",
                new BigDecimal("300000"), BigDecimal.ZERO, new BigDecimal("20000"), new BigDecimal("320000"),
                null, null, List.of(), Instant.now(), Instant.now(), null, null
        );
    }
}
