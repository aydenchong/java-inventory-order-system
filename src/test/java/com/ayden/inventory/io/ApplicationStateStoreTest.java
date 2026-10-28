
package com.ayden.inventory.io;

import com.ayden.inventory.domain.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationStateStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRestoreInventoryAndAllOrderStatuses() throws IOException {

        OrderService service = new OrderService();

        service.registerProduct(
                new Product("P001", "Mouse", new BigDecimal("89.00")), 20
        );
        service.registerProduct(
                new Product("P002", "Keyboard", new BigDecimal("199.00")), 7
        );

        service.placeOrder("ORD001", Map.of("P001", 3, "P002", 2));

        service.placeOrder("ORD002", Map.of("P001", 5));
        service.fulfillOrder("ORD002");

        service.placeOrder("ORD003", Map.of("P002", 1));
        service.cancelOrder("ORD003");

        Path file = tempDir.resolve("state.properties");

        ApplicationStateStore.save(service, file);

        OrderService restored = ApplicationStateStore.load(file);

        assertEquals(OrderStatus.RESERVED,
                restored.getOrder("ORD001").getStatus());
        assertEquals(OrderStatus.FULFILLED,
                restored.getOrder("ORD002").getStatus());
        assertEquals(OrderStatus.CANCELLED,
                restored.getOrder("ORD003").getStatus());

        assertEquals(15, restored.getStock("P001").onHand());
        assertEquals(3, restored.getStock("P001").reserved());
        assertEquals(7, restored.getStock("P002").onHand());
        assertEquals(2, restored.getStock("P002").reserved());

        // Verify that a restored order remains functional.
        restored.fulfillOrder("ORD001");

        assertEquals(12, restored.getStock("P001").onHand());
        assertEquals(0, restored.getStock("P001").reserved());
    }

    @Test
    void shouldPreserveSpecialCharactersAndUnicode() throws IOException {

        OrderService service = new OrderService();

        service.registerProduct(
                new Product(
                        "P001",
                        "Mouse, \"Pro\" 中文",
                        new BigDecimal("89.00")
                ),
                10
        );

        Path file = tempDir.resolve("state.properties");

        ApplicationStateStore.save(service, file);

        OrderService restored = ApplicationStateStore.load(file);

        assertEquals(
                "Mouse, \"Pro\" 中文",
                restored.listProducts().get(0).getName()
        );
    }

    @Test
    void shouldRejectInconsistentInventory() throws IOException {

        OrderService service = new OrderService();

        service.registerProduct(
                new Product("P001", "Mouse", BigDecimal.TEN), 10
        );

        Path file = tempDir.resolve("state.properties");

        ApplicationStateStore.save(service, file);

        Properties properties = new Properties();

        try (var reader = Files.newBufferedReader(
                file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        properties.setProperty("product.0.reserved", "99");

        try (var writer = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8)) {
            properties.store(writer, "Corrupted test file");
        }

        assertThrows(IOException.class,
                () -> ApplicationStateStore.load(file));

        // The original service remains unchanged.
        assertEquals(10, service.getStock("P001").available());
    }

    @Test
    void shouldRejectMismatchedHistoricalPrice() throws IOException {

        OrderService service = new OrderService();

        service.registerProduct(
                new Product("P001", "Mouse", new BigDecimal("89.00")),
                10
        );

        service.placeOrder("ORD001", Map.of("P001", 2));

        Path file = tempDir.resolve("state.properties");

        ApplicationStateStore.save(service, file);

        Properties properties = new Properties();

        try (var reader = Files.newBufferedReader(
                file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        properties.setProperty("order.0.item.0.unitPrice", "999.00");

        try (var writer = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8)) {
            properties.store(writer, "Tampered test file");
        }

        assertThrows(IOException.class,
                () -> ApplicationStateStore.load(file));
    }
}
