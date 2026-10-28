
package com.ayden.inventory.io;

import com.ayden.inventory.domain.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RestockPersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRestockWithoutChangingReservations() {

        OrderService service = createService(20);

        service.placeOrder("ORD001", Map.of("P001", 5));

        service.restockProduct("P001", 10);

        InventorySnapshot stock = service.getStock("P001");

        assertEquals(30, stock.onHand());
        assertEquals(5, stock.reserved());
        assertEquals(25, stock.available());
    }

    @Test
    void shouldRejectInvalidRestockWithoutChangingStock() {

        OrderService service = createService(20);

        assertThrows(IllegalArgumentException.class,
                () -> service.restockProduct("P001", 0));

        assertThrows(IllegalArgumentException.class,
                () -> service.restockProduct("P001", -5));

        assertThrows(IllegalArgumentException.class,
                () -> service.restockProduct("P999", 10));

        assertThrows(IllegalArgumentException.class,
                () -> service.restockProduct(
                        "P001", Integer.MAX_VALUE
                ));

        assertEquals(20, service.getStock("P001").onHand());
        assertEquals(0, service.getStock("P001").reserved());
    }

    @Test
    void shouldPreserveRestockedInventoryAfterReload()
            throws IOException {

        OrderService service = createService(20);

        service.placeOrder("ORD001", Map.of("P001", 5));
        service.restockProduct("P001", 10);

        Path file = tempDir.resolve("state.properties");

        ApplicationStateStore.save(service, file);

        OrderService restored = ApplicationStateStore.load(file);

        assertEquals(30, restored.getStock("P001").onHand());
        assertEquals(5, restored.getStock("P001").reserved());
        assertEquals(25, restored.getStock("P001").available());

        restored.fulfillOrder("ORD001");

        assertEquals(25, restored.getStock("P001").onHand());
        assertEquals(0, restored.getStock("P001").reserved());
    }

    @Test
    void shouldRestoreInventoryAfterMultipleLargeFulfillments()
            throws IOException {

        int max = Integer.MAX_VALUE;

        OrderService service = createService(max);

        service.placeOrder(
                "ORD001", Map.of("P001", max)
        );
        service.fulfillOrder("ORD001");

        service.restockProduct("P001", max);

        service.placeOrder(
                "ORD002", Map.of("P001", max)
        );
        service.fulfillOrder("ORD002");

        service.restockProduct("P001", max);

        Path file = tempDir.resolve("state.properties");

        ApplicationStateStore.save(service, file);

        OrderService restored = ApplicationStateStore.load(file);

        assertEquals(max, restored.getStock("P001").onHand());
        assertEquals(0, restored.getStock("P001").reserved());

        assertEquals(OrderStatus.FULFILLED,
                restored.getOrder("ORD001").getStatus());

        assertEquals(OrderStatus.FULFILLED,
                restored.getOrder("ORD002").getStatus());
    }

    private OrderService createService(int quantity) {

        OrderService service = new OrderService();

        service.registerProduct(
                new Product(
                        "P001",
                        "Gaming Mouse",
                        new BigDecimal("89.00")
                ),
                quantity
        );

        return service;
    }
}
