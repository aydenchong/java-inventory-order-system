package com.ayden.inventory.io;

import com.ayden.inventory.domain.OrderService;
import com.ayden.inventory.domain.Product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventoryCsvExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExportCurrentInventory() throws Exception {

        OrderService service = new OrderService();

        service.registerProduct(
                new Product("P001", "Gaming Mouse", new BigDecimal("89.00")),
                10
        );

        service.registerProduct(
                new Product("P002", "Keyboard", new BigDecimal("199.00")),
                5
        );

        service.placeOrder(
                "ORD001", Map.of("P001", 2, "P002", 1)
        );

        Path file = tempDir.resolve("inventory.csv");

        int count = InventoryCsvExporter.export(service, file);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        assertEquals(2, count);
        assertEquals(3, lines.size());

        assertEquals(
                "product_id,product_name,unit_price_rm,on_hand,reserved,available",
                lines.get(0)
        );

        assertEquals(
                "\"P001\",\"Gaming Mouse\",89.00,10,2,8",
                lines.get(1)
        );

        assertEquals(
                "\"P002\",\"Keyboard\",199.00,5,1,4",
                lines.get(2)
        );
    }

    @Test
    void shouldEscapeSpecialCharacters() throws Exception {

        OrderService service = new OrderService();

        service.registerProduct(
                new Product(
                        "P003",
                        "Mouse, \"Pro\"",
                        new BigDecimal("10.50")
                ),
                4
        );

        Path file = tempDir.resolve("nested/inventory.csv");

        InventoryCsvExporter.export(service, file);

        String csv = Files.readString(file, StandardCharsets.UTF_8);

        assertTrue(Files.exists(file));
        assertTrue(
                csv.contains("\"P003\",\"Mouse, \"\"Pro\"\"\",10.50,4,0,4")
        );
    }

    @Test
    void shouldProtectFormulaLikeProductNames() throws Exception {

        OrderService service = new OrderService();

        service.registerProduct(
                new Product("P004", "=1+1", BigDecimal.TEN),
                2
        );

        Path file = tempDir.resolve("inventory.csv");

        InventoryCsvExporter.export(service, file);

        String csv = Files.readString(file, StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"P004\",\"'=1+1\",10,2,0,2"));
    }
}

