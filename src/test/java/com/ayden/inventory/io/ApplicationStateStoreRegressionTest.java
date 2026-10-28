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

class ApplicationStateStoreRegressionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldContinueOrderOperationsAfterReload() throws IOException {

        OrderService service = new OrderService();

        service.registerProduct(
                new Product("P001", "Mouse", BigDecimal.TEN),
                20
        );

        service.placeOrder("ORD-OPEN", Map.of("P001", 4));

        service.placeOrder("ORD-DONE", Map.of("P001", 6));
        service.fulfillOrder("ORD-DONE");

        service.placeOrder("ORD-CANCEL", Map.of("P001", 2));
        service.cancelOrder("ORD-CANCEL");

        Path file = tempDir.resolve("state.properties");

        ApplicationStateStore.save(service, file);

        OrderService restored = ApplicationStateStore.load(file);

        assertEquals(14, restored.getStock("P001").onHand());
        assertEquals(4, restored.getStock("P001").reserved());

        restored.cancelOrder("ORD-OPEN");

        assertEquals(14, restored.getStock("P001").available());
        assertEquals(0, restored.getStock("P001").reserved());

        assertEquals(OrderStatus.FULFILLED,
                restored.getOrder("ORD-DONE").getStatus());

        assertEquals(OrderStatus.CANCELLED,
                restored.getOrder("ORD-CANCEL").getStatus());
    }

    @Test
    void shouldRejectReservationWithoutCorrespondingOrder()
            throws IOException {

        OrderService service = new OrderService();

        service.registerProduct(
                new Product("P001", "Mouse", BigDecimal.TEN),
                10
        );

        Path file = tempDir.resolve("state.properties");

        ApplicationStateStore.save(service, file);

        Properties properties = new Properties();

        try (var reader = Files.newBufferedReader(
                file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        // Corrupt the saved reserved quantity.
        properties.setProperty("product.0.reserved", "1");

        try (var writer = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8)) {
            properties.store(writer, "Test corrupted state");
        }

        assertThrows(IOException.class,
                () -> ApplicationStateStore.load(file));
    }

    @Test
    void shouldRejectUnsupportedStateVersion() throws IOException {

        OrderService service = new OrderService();

        Path file = tempDir.resolve("state.properties");

        ApplicationStateStore.save(service, file);

        Properties properties = new Properties();

        try (var reader = Files.newBufferedReader(
                file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        properties.setProperty("version", "999");

        try (var writer = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8)) {
            properties.store(writer, "Unsupported version");
        }

        assertThrows(IOException.class,
                () -> ApplicationStateStore.load(file));
    }
}
