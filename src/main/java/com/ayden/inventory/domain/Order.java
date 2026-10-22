package com.ayden.inventory.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Order {

    private final String orderId;
    private final List<OrderItem> items;
    private OrderStatus status;

    public Order(String orderId, List<OrderItem> items) {

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Invalid order ID!");
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item!");
        }

        Set<String> productIds = new HashSet<>();

        for (OrderItem item : items) {

            if (item == null) {
                throw new IllegalArgumentException("Order item cannot be null!");
            }

            if (!productIds.add(item.getProductId())) {
                throw new IllegalArgumentException(
                        "Duplicate product in order: " + item.getProductId()
                );
            }
        }

        this.orderId = orderId;
        this.items = List.copyOf(items);
        this.status = OrderStatus.CREATED;
    }

    public String getOrderId() {
        return orderId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem item : items) {
            total = total.add(item.getSubtotal());
        }

        return total;
    }
}
