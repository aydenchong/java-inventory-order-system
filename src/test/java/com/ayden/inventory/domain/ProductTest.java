package com.ayden.inventory.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void shouldCreateValidProduct() {
        Product product = new Product(
                "P001",
                "Gaming Mouse",
                new BigDecimal("89.00")
        );

        assertEquals("P001", product.getProductId());
        assertEquals("Gaming Mouse", product.getName());
        assertEquals(new BigDecimal("89.00"), product.getPrice());
    }

    @Test
    void shouldRejectInvalidProductId() {
        assertThrows(IllegalArgumentException.class,
                () -> new Product(null, "Mouse", BigDecimal.TEN));

        assertThrows(IllegalArgumentException.class,
                () -> new Product("  ", "Mouse", BigDecimal.TEN));
    }

    @Test
    void shouldRejectInvalidProductName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Product("P001", null, BigDecimal.TEN));

        assertThrows(IllegalArgumentException.class,
                () -> new Product("P001", "  ", BigDecimal.TEN));
    }

    @Test
    void shouldRejectNonPositivePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new Product("P001", "Mouse", BigDecimal.ZERO));

        assertThrows(IllegalArgumentException.class,
                () -> new Product("P001", "Mouse", new BigDecimal("-10.00")));
    }

    @Test
    void shouldRejectNullPrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new Product("P001", "Mouse", null));
    }
}
