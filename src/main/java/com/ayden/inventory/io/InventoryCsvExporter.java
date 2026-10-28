package com.ayden.inventory.io;

import com.ayden.inventory.domain.InventorySnapshot;
import com.ayden.inventory.domain.OrderService;
import com.ayden.inventory.domain.Product;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class InventoryCsvExporter {

    private InventoryCsvExporter() {
    }

    public static int export(OrderService service, Path outputFile)
            throws IOException {

        List<Product> products = service.listProducts();

        Path parent = outputFile.toAbsolutePath().getParent();
        Files.createDirectories(parent);

        try (BufferedWriter writer = Files.newBufferedWriter(
                outputFile, StandardCharsets.UTF_8)) {

            writer.write(
                    "product_id,product_name,unit_price_rm,on_hand,reserved,available"
            );
            writer.newLine();

            for (Product product : products) {

                InventorySnapshot stock =
                        service.getStock(product.getProductId());

                String line = String.join(",",
                        csvText(product.getProductId()),
                        csvText(product.getName()),
                        product.getPrice().toPlainString(),
                        Integer.toString(stock.onHand()),
                        Integer.toString(stock.reserved()),
                        Integer.toString(stock.available())
                );

                writer.write(line);
                writer.newLine();
            }
        }

        return products.size();
    }

    private static String csvText(String value) {

        String safe = value;
        String trimmed = value.stripLeading();

        // Prevent spreadsheet formula injection.
        if (!trimmed.isEmpty()
                && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
            safe = "'" + value;
        }

        // Escape quotation marks for CSV.
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
