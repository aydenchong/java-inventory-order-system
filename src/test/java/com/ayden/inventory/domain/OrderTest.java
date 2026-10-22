package com.ayden.inventory.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void shouldCalculateOrderItemSubtotal() {
        OrderItem item = new OrderItem(
                "P001", 2, new BigDecimal("89.00")
        );

        assertEquals(new BigDecimal("178.00"), item.getSubtotal());
    }

    @Test
    void shouldCreateOrderWithCorrectTotal() {
        OrderItem mouse = new OrderItem(
                "P001", 2, new BigDecimal("89.00")
        );

        OrderItem keyboard = new OrderItem(
                "P002", 1, new BigDecimal("199.00")
        );

        Order order = new Order(
                "ORD001",
                List.of(mouse, keyboard)
        );

        assertEquals("ORD001", order.getOrderId());
        assertEquals(2, order.getItems().size());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(new BigDecimal("377.00"), order.getTotalAmount());
    }

    @Test
    void shouldRejectEmptyOrder() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order("ORD001", List.of()));
    }

    @Test
    void shouldRejectInvalidOrderId() {
        OrderItem item = new OrderItem("P001", 1, BigDecimal.TEN);

        assertThrows(IllegalArgumentException.class,
                () -> new Order("  ", List.of(item)));
    }

    @Test
    void shouldRejectDuplicateProducts() {
        OrderItem first = new OrderItem("P001", 2, BigDecimal.TEN);
        OrderItem second = new OrderItem("P001", 3, BigDecimal.TEN);

        assertThrows(IllegalArgumentException.class,
                () -> new Order("ORD001", List.of(first, second)));
    }

    @Test
    void shouldRejectInvalidOrderItem() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("P001", 0, BigDecimal.TEN));

        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("P001", 1, BigDecimal.ZERO));

        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem(null, 1, BigDecimal.TEN));
    }

    @Test
    void shouldProtectOrderItemsFromModification() {
        OrderItem item = new OrderItem("P001", 1, BigDecimal.TEN);

        List<OrderItem> originalItems = new ArrayList<>();
        originalItems.add(item);

        Order order = new Order("ORD001", originalItems);

        originalItems.clear();

        assertEquals(1, order.getItems().size());

        assertThrows(UnsupportedOperationException.class,
                () -> order.getItems().clear());
    }
}
