
package com.ayden.inventory.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderService {

    private final Map<String, Product> products = new HashMap<>();
    private final Map<String, Inventory> inventories = new HashMap<>();
    private final Map<String, Order> orders = new HashMap<>();

    public void registerProduct(Product product, int initialQuantity) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null!");
        }

        if (products.containsKey(product.getProductId())) {
            throw new IllegalStateException(
                    "Product already registered: " + product.getProductId()
            );
        }

        Inventory inventory = new Inventory(
                product.getProductId(), initialQuantity
        );

        products.put(product.getProductId(), product);
        inventories.put(product.getProductId(), inventory);
    }

    public Order placeOrder(
            String orderId,
            Map<String, Integer> requestedQuantities
    ) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Invalid order ID!");
        }

        if (orders.containsKey(orderId)) {
            throw new IllegalStateException(
                    "Order ID already exists: " + orderId
            );
        }

        if (requestedQuantities == null || requestedQuantities.isEmpty()) {
            throw new IllegalArgumentException(
                    "Order must contain products!"
            );
        }

        // Phase 1: Validate all items before reserving stock.
        List<OrderItem> items = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : requestedQuantities.entrySet()) {
            String productId = entry.getKey();
            Product product = products.get(productId);

            if (product == null) {
                throw new IllegalArgumentException(
                        "Unknown product: " + productId
                );
            }

            Integer quantity = entry.getValue();

            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException(
                        "Invalid quantity for: " + productId
                );
            }

            Inventory inventory = inventories.get(productId);

            if (quantity > inventory.getAvailableQuantity()) {
                throw new IllegalStateException(
                        "Insufficient stock for: " + productId
                );
            }

            items.add(new OrderItem(
                    productId, quantity, product.getPrice()
            ));
        }

        items.sort(Comparator.comparing(OrderItem::getProductId));

        Order order = new Order(orderId, items);

        // Phase 2: Reserve only after validation succeeds.
        for (OrderItem item : order.getItems()) {
            inventories.get(item.getProductId())
                    .reserveStock(item.getQuantity());
        }

        order.markReserved();
        orders.put(orderId, order);

        return order;
    }

    public void cancelOrder(String orderId) {
        Order order = requireReservedOrder(orderId);
        ensureReservationsPresent(order);

        for (OrderItem item : order.getItems()) {
            inventories.get(item.getProductId())
                    .releaseStock(item.getQuantity());
        }

        order.markCancelled();
    }

    public void fulfillOrder(String orderId) {
        Order order = requireReservedOrder(orderId);
        ensureReservationsPresent(order);

        for (OrderItem item : order.getItems()) {
            inventories.get(item.getProductId())
                    .fulfillStock(item.getQuantity());
        }

        order.markFulfilled();
    }

    public Order getOrder(String orderId) {
        Order order = orders.get(orderId);

        if (order == null) {
            throw new IllegalArgumentException(
                    "Unknown order: " + orderId
            );
        }

        return order;
    }

    public InventorySnapshot getStock(String productId) {
        Inventory inventory = inventories.get(productId);

        if (inventory == null) {
            throw new IllegalArgumentException(
                    "Unknown product: " + productId
            );
        }

        return new InventorySnapshot(
                inventory.getQuantityOnHand(),
                inventory.getQuantityReserved(),
                inventory.getAvailableQuantity()
        );
    }

    private Order requireReservedOrder(String orderId) {
        Order order = getOrder(orderId);

        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException(
                    "Order must be RESERVED: " + orderId
            );
        }

        return order;
    }

    private void ensureReservationsPresent(Order order) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventories.get(item.getProductId());

            if (inventory.getQuantityReserved() < item.getQuantity()) {
                throw new IllegalStateException(
                        "Missing reservation for: " + item.getProductId()
                );
            }
        }
    }
}
