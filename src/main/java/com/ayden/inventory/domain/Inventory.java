
package com.ayden.inventory.domain;

public class Inventory {

    private final String productId;
    private int quantityOnHand;
    private int quantityReserved;

    public Inventory(String productId, int initialQuantity) {

        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Invalid product ID!");
        }

        if (initialQuantity < 0) {
            throw new IllegalArgumentException("Initial quantity cannot be negative!");
        }

        this.productId = productId;
        this.quantityOnHand = initialQuantity;
        this.quantityReserved = 0;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public int getAvailableQuantity() {
        return quantityOnHand - quantityReserved;
    }

    public void restock(int quantity) {
        validatePositiveQuantity(quantity);

        if (quantity > Integer.MAX_VALUE - quantityOnHand) {
            throw new IllegalArgumentException("Stock quantity exceeds supported maximum!");
        }

        quantityOnHand += quantity;
    }

    public void reserveStock(int quantity) {
        validatePositiveQuantity(quantity);

        if (quantity > getAvailableQuantity()) {
            throw new IllegalStateException("Insufficient available stock!");
        }

        quantityReserved += quantity;
    }

    public void releaseStock(int quantity) {
        validatePositiveQuantity(quantity);

        if (quantity > quantityReserved) {
            throw new IllegalStateException("Cannot release more than reserved stock!");
        }

        quantityReserved -= quantity;
    }

    public void fulfillStock(int quantity) {
        validatePositiveQuantity(quantity);

        if (quantity > quantityReserved) {
            throw new IllegalStateException("Cannot fulfill more than reserved stock!");
        }

        quantityOnHand -= quantity;
        quantityReserved -= quantity;
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero!");
        }
    }
}
