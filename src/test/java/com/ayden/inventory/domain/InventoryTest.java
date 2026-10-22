package com.ayden.inventory.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new Inventory("P001", 20);
    }

    @Test
    void shouldInitializeInventory() {
        assertEquals(20, inventory.getQuantityOnHand());
        assertEquals(0, inventory.getQuantityReserved());
        assertEquals(20, inventory.getAvailableQuantity());
    }

    @Test
    void shouldReserveStock() {
        inventory.reserveStock(5);

        assertEquals(20, inventory.getQuantityOnHand());
        assertEquals(5, inventory.getQuantityReserved());
        assertEquals(15, inventory.getAvailableQuantity());
    }

    @Test
    void shouldRejectInsufficientStock() {
        assertThrows(IllegalStateException.class,
                () -> inventory.reserveStock(25));

        assertEquals(20, inventory.getAvailableQuantity());
        assertEquals(0, inventory.getQuantityReserved());
    }

    @Test
    void shouldFulfillReservedStock() {
        inventory.reserveStock(5);
        inventory.fulfillStock(3);

        assertEquals(17, inventory.getQuantityOnHand());
        assertEquals(2, inventory.getQuantityReserved());
        assertEquals(15, inventory.getAvailableQuantity());
    }

    @Test
    void shouldReleaseReservedStock() {
        inventory.reserveStock(5);
        inventory.releaseStock(5);

        assertEquals(20, inventory.getQuantityOnHand());
        assertEquals(0, inventory.getQuantityReserved());
        assertEquals(20, inventory.getAvailableQuantity());
    }

    @Test
    void shouldRestockInventory() {
        inventory.restock(10);

        assertEquals(30, inventory.getQuantityOnHand());
        assertEquals(30, inventory.getAvailableQuantity());
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> inventory.restock(0));

        assertThrows(IllegalArgumentException.class,
                () -> inventory.reserveStock(-1));

        assertThrows(IllegalArgumentException.class,
                () -> inventory.releaseStock(0));

        assertThrows(IllegalArgumentException.class,
                () -> inventory.fulfillStock(-1));
    }

    @Test
    void shouldRejectExcessiveReleaseAndFulfillment() {
        inventory.reserveStock(5);

        assertThrows(IllegalStateException.class,
                () -> inventory.releaseStock(6));

        assertThrows(IllegalStateException.class,
                () -> inventory.fulfillStock(6));

        assertEquals(20, inventory.getQuantityOnHand());
        assertEquals(5, inventory.getQuantityReserved());
    }

    @Test
    void shouldRejectStockOverflow() {
        Inventory fullInventory =
                new Inventory("P002", Integer.MAX_VALUE);

        assertThrows(IllegalArgumentException.class,
                () -> fullInventory.restock(1));

        assertEquals(Integer.MAX_VALUE,
                fullInventory.getQuantityOnHand());
    }
}
