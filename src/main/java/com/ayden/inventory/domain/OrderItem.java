package com.ayden.inventory.domain;

import java.math.BigDecimal;

public class OrderItem {

    private final String productId;
    private final int quantity;
    private final BigDecimal unitPrice;

    public OrderItem(String productId, int quantity, BigDecimal unitPrice) {

        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Invalid product ID!");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero!");
        }

        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero!");
        }

        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
