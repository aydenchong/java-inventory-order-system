package com.ayden.inventory.io;

import com.ayden.inventory.domain.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationStateReplayOrderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRestoreOrdersRegardlessOfIdOrder() throws IOException {

        OrderService service = new OrderService();

        service.registerProduct(
                new Product("P001", "Mouse", BigDecimal.TEN),
                10
        );

        // First: reserve all stock, then cancel.
        service.placeOrder("Z-CANCEL", Map.of("P001", 10));
        service.cancelOrder("Z-CANCEL");

        // Second: fulfill seven items.
        service.placeOrder("B-FULFILL", Map.of("P001", 7));
        service.fulfillOrder("B-FULFILL");

        // Third: reserve the remaining three items.
        service.placeOrder("A-RESERVE", Map.of("P001", 3));

        Path file = tempDir.resolve("state.properties");

        ApplicationStateStore.save(service, file);

        OrderService restored = ApplicationStateStore.load(file);

        assertEquals(
                OrderStatus.CANCELLED,
                restored.getOrder("Z-CANCEL").getStatus()
        );

        assertEquals(
                OrderStatus.FULFILLED,
                restored.getOrder("B-FULFILL").getStatus()
        );

        assertEquals(
                OrderStatus.RESERVED,
                restored.getOrder("A-RESERVE").getStatus()
        );

        InventorySnapshot stock = restored.getStock("P001");

        assertEquals(3, stock.onHand());
        assertEquals(3, stock.reserved());
        assertEquals(0, stock.available());

        // Restored orders must still support business operations.
        restored.cancelOrder("A-RESERVE");

        assertEquals(3, restored.getStock("P001").available());
    }
}
