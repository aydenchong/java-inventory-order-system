package com.ayden.inventory.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceRegressionTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService();

        service.registerProduct(
                new Product("P001", "Mouse", new BigDecimal("89.00")),
                20
        );

        service.registerProduct(
                new Product("P002", "Keyboard", new BigDecimal("199.00")),
                10
        );
    }

    @Test
    void shouldMaintainStockAcrossMultipleOrders() {

        service.placeOrder(
                "ORD001", Map.of("P001", 5, "P002", 3)
        );

        service.placeOrder(
                "ORD002", Map.of("P001", 4)
        );

        service.placeOrder(
                "ORD003", Map.of("P002", 2)
        );

        service.fulfillOrder("ORD001");
        service.cancelOrder("ORD002");
        service.fulfillOrder("ORD003");

        InventorySnapshot mouse = service.getStock("P001");
        InventorySnapshot keyboard = service.getStock("P002");

        assertEquals(15, mouse.onHand());
        assertEquals(0, mouse.reserved());
        assertEquals(15, mouse.available());

        assertEquals(5, keyboard.onHand());
        assertEquals(0, keyboard.reserved());
        assertEquals(5, keyboard.available());

        assertEquals(OrderStatus.FULFILLED,
                service.getOrder("ORD001").getStatus());

        assertEquals(OrderStatus.CANCELLED,
                service.getOrder("ORD002").getStatus());

        assertEquals(OrderStatus.FULFILLED,
                service.getOrder("ORD003").getStatus());
    }

    @Test
    void shouldAllowReusingStockAfterCancellation() {

        service.placeOrder("ORD001", Map.of("P001", 20));

        assertEquals(0, service.getStock("P001").available());

        assertThrows(IllegalStateException.class,
                () -> service.placeOrder(
                        "ORD002", Map.of("P001", 1)
                ));

        service.cancelOrder("ORD001");

        assertEquals(20, service.getStock("P001").available());

        service.placeOrder("ORD003", Map.of("P001", 20));
        service.fulfillOrder("ORD003");

        assertEquals(0, service.getStock("P001").onHand());
        assertEquals(0, service.getStock("P001").reserved());
    }

    @Test
    void shouldAllowRetryWithSameIdAfterFailedOrder() {

        assertThrows(IllegalStateException.class,
                () -> service.placeOrder(
                        "ORD001", Map.of("P001", 21)
                ));

        Order successful = service.placeOrder(
                "ORD001", Map.of("P001", 2)
        );

        assertEquals(OrderStatus.RESERVED, successful.getStatus());
        assertEquals(18, service.getStock("P001").available());
    }

    @Test
    void shouldRejectInvalidRegistrationWithoutSideEffects() {

        Product cable = new Product(
                "P003", "USB-C Cable", new BigDecimal("29.00")
        );

        assertThrows(IllegalArgumentException.class,
                () -> service.registerProduct(cable, -1));

        assertThrows(IllegalArgumentException.class,
                () -> service.getStock("P003"));

        service.registerProduct(cable, 5);

        assertEquals(5, service.getStock("P003").available());
    }

    @Test
    void shouldReturnSortedReadOnlyLists() {

        service.registerProduct(
                new Product("P000", "Adapter", BigDecimal.TEN),
                5
        );

        List<Product> products = service.listProducts();

        assertEquals(
                List.of("P000", "P001", "P002"),
                products.stream().map(Product::getProductId).toList()
        );

        assertThrows(UnsupportedOperationException.class,
                products::clear);

        service.placeOrder("ORD-B", Map.of("P001", 1));
        service.placeOrder("ORD-A", Map.of("P002", 1));

        assertEquals(
                List.of("ORD-A", "ORD-B"),
                service.listOrders().stream()
                        .map(Order::getOrderId)
                        .toList()
        );
    }
}
