package com.ayden.inventory.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService();

        service.registerProduct(
                new Product("P001", "Mouse", new BigDecimal("89.00")), 10
        );

        service.registerProduct(
                new Product("P002", "Keyboard", new BigDecimal("199.00")), 5
        );
    }

    @Test
    void shouldRegisterProductsWithInitialStock() {
        InventorySnapshot stock = service.getStock("P001");

        assertEquals(10, stock.onHand());
        assertEquals(0, stock.reserved());
        assertEquals(10, stock.available());
    }

    @Test
    void shouldReserveAllItemsAndCalculateTotal() {
        Order order = service.placeOrder(
                "ORD001", Map.of("P001", 2, "P002", 1)
        );

        assertEquals(OrderStatus.RESERVED, order.getStatus());
        assertEquals(new BigDecimal("377.00"), order.getTotalAmount());
        assertEquals(8, service.getStock("P001").available());
        assertEquals(4, service.getStock("P002").available());
    }

    @Test
    void shouldRejectOrderWithoutPartialReservation() {
        assertThrows(IllegalStateException.class,
                () -> service.placeOrder(
                        "ORD001", Map.of("P001", 2, "P002", 10)
                ));

        assertEquals(10, service.getStock("P001").available());
        assertEquals(5, service.getStock("P002").available());

        assertThrows(IllegalArgumentException.class,
                () -> service.getOrder("ORD001"));
    }

    @Test
    void shouldRejectUnknownProductWithoutChangingStock() {
        assertThrows(IllegalArgumentException.class,
                () -> service.placeOrder(
                        "ORD001", Map.of("P001", 2, "P999", 1)
                ));

        assertEquals(10, service.getStock("P001").available());
    }

    @Test
    void shouldFulfillOrder() {
        Order order = service.placeOrder(
                "ORD001", Map.of("P001", 3)
        );

        service.fulfillOrder("ORD001");

        assertEquals(OrderStatus.FULFILLED, order.getStatus());
        assertEquals(7, service.getStock("P001").onHand());
        assertEquals(0, service.getStock("P001").reserved());
        assertEquals(7, service.getStock("P001").available());
    }

    @Test
    void shouldCancelOrder() {
        Order order = service.placeOrder(
                "ORD001", Map.of("P001", 3)
        );

        service.cancelOrder("ORD001");

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(10, service.getStock("P001").onHand());
        assertEquals(0, service.getStock("P001").reserved());
        assertEquals(10, service.getStock("P001").available());
    }

    @Test
    void shouldRejectRepeatedFulfillmentAndCancellation() {
        service.placeOrder("ORD001", Map.of("P001", 2));
        service.fulfillOrder("ORD001");

        assertThrows(IllegalStateException.class,
                () -> service.fulfillOrder("ORD001"));

        assertThrows(IllegalStateException.class,
                () -> service.cancelOrder("ORD001"));

        assertEquals(8, service.getStock("P001").onHand());
    }

    @Test
    void shouldRejectDuplicateOrderId() {
        service.placeOrder("ORD001", Map.of("P001", 2));

        assertThrows(IllegalStateException.class,
                () -> service.placeOrder(
                        "ORD001", Map.of("P001", 3)
                ));

        assertEquals(8, service.getStock("P001").available());
    }

    @Test
    void shouldPreventOversellingAcrossOrders() {
        service.placeOrder("ORD001", Map.of("P001", 7));

        assertThrows(IllegalStateException.class,
                () -> service.placeOrder(
                        "ORD002", Map.of("P001", 4)
                ));

        assertEquals(3, service.getStock("P001").available());
    }

    @Test
    void shouldRejectInvalidOrderQuantities() {
        Map<String, Integer> quantities = new HashMap<>();
        quantities.put("P001", 0);

        assertThrows(IllegalArgumentException.class,
                () -> service.placeOrder("ORD001", quantities));

        quantities.put("P001", null);

        assertThrows(IllegalArgumentException.class,
                () -> service.placeOrder("ORD002", quantities));

        assertEquals(10, service.getStock("P001").available());
    }
}
