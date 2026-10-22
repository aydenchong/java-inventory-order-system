package com.ayden.inventory;

import com.ayden.inventory.domain.*;

import java.math.BigDecimal;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        OrderService service = new OrderService();

        service.registerProduct(
                new Product("P001", "Gaming Mouse", new BigDecimal("89.00")), 10
        );

        service.registerProduct(
                new Product("P002", "Mechanical Keyboard", new BigDecimal("199.00")), 5
        );

        service.registerProduct(
                new Product("P003", "USB-C Cable", new BigDecimal("29.00")), 30
        );

        System.out.println("=== INITIAL STOCK ===");
        printStock(service, "P001");
        printStock(service, "P002");

        // Create and reserve an order.
        Order first = service.placeOrder(
                "ORD001",
                Map.of("P001", 2, "P002", 1)
        );

        System.out.println("\nPlaced " + first.getOrderId()
                + " | " + first.getStatus()
                + " | RM" + first.getTotalAmount());

        printStock(service, "P001");
        printStock(service, "P002");

        // Reject an order with insufficient stock.
        try {
            service.placeOrder(
                    "ORD002",
                    Map.of("P001", 1, "P002", 999)
            );
        } catch (IllegalStateException e) {
            System.out.println("\nRejected order: " + e.getMessage());
        }

        printStock(service, "P001");
        printStock(service, "P002");

        // Fulfill the first order.
        service.fulfillOrder("ORD001");

        System.out.println("\nAfter fulfillment: " + first.getStatus());
        printStock(service, "P001");
        printStock(service, "P002");

        // Create another order and cancel it.
        Order second = service.placeOrder(
                "ORD003",
                Map.of("P001", 1)
        );

        service.cancelOrder(second.getOrderId());

        System.out.println("\nAfter cancellation: " + second.getStatus());
        printStock(service, "P001");
    }

    private static void printStock(
            OrderService service, String productId
    ) {
        InventorySnapshot stock = service.getStock(productId);

        System.out.printf(
                "%s | On Hand: %d | Reserved: %d | Available: %d%n",
                productId,
                stock.onHand(),
                stock.reserved(),
                stock.available()
        );
    }
}
