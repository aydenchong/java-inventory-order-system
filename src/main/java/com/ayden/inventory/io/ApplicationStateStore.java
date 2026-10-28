package com.ayden.inventory.io;

import com.ayden.inventory.domain.*;
import java.util.Comparator;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class ApplicationStateStore {

    private static final String VERSION = "1";

    private ApplicationStateStore() {
    }

    public static void save(OrderService service, Path file)
            throws IOException {

        Properties p = new Properties();
        p.setProperty("version", VERSION);

        List<Product> products = service.listProducts();

        p.setProperty("product.count",
                Integer.toString(products.size()));

        for (int i = 0; i < products.size(); i++) {

            Product product = products.get(i);
            InventorySnapshot stock =
                    service.getStock(product.getProductId());

            String key = "product." + i + ".";

            p.setProperty(key + "id", product.getProductId());
            p.setProperty(key + "name", product.getName());
            p.setProperty(key + "price",
                    product.getPrice().toPlainString());
            p.setProperty(key + "onHand",
                    Integer.toString(stock.onHand()));
            p.setProperty(key + "reserved",
                    Integer.toString(stock.reserved()));
        }

        List<Order> orders = service.listOrders();

        p.setProperty("order.count",
                Integer.toString(orders.size()));

        for (int i = 0; i < orders.size(); i++) {

            Order order = orders.get(i);
            String key = "order." + i + ".";

            p.setProperty(key + "id", order.getOrderId());
            p.setProperty(key + "status",
                    order.getStatus().name());
            p.setProperty(key + "itemCount",
                    Integer.toString(order.getItems().size()));

            for (int j = 0; j < order.getItems().size(); j++) {

                OrderItem item = order.getItems().get(j);
                String itemKey = key + "item." + j + ".";

                p.setProperty(itemKey + "productId",
                        item.getProductId());
                p.setProperty(itemKey + "quantity",
                        Integer.toString(item.getQuantity()));
                p.setProperty(itemKey + "unitPrice",
                        item.getUnitPrice().toPlainString());
            }
        }

        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();

        Files.createDirectories(parent);

        Path temporary = Files.createTempFile(
                parent, "app-state-", ".tmp"
        );

        try {
            try (Writer writer = Files.newBufferedWriter(
                    temporary, StandardCharsets.UTF_8)) {

                p.store(writer, "Inventory & Order Management State");
            }

            try {
                Files.move(
                        temporary,
                        absolute,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(
                        temporary,
                        absolute,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static OrderService load(Path file) throws IOException {

        Properties p = new Properties();

        try (Reader reader = Files.newBufferedReader(
                file, StandardCharsets.UTF_8)) {

            p.load(reader);

        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid state file encoding", e);
        }

        try {
            if (!VERSION.equals(required(p, "version"))) {
                throw new IllegalArgumentException(
                        "Unsupported state file version"
                );
            }

            Map<String, SavedProduct> products =
                    new LinkedHashMap<>();

            int productCount = count(p, "product.count", false);

            for (int i = 0; i < productCount; i++) {

                String key = "product." + i + ".";

                Product product = new Product(
                        required(p, key + "id"),
                        required(p, key + "name"),
                        new BigDecimal(required(p, key + "price"))
                );

                int onHand = nonNegativeInt(p, key + "onHand");
                int reserved = nonNegativeInt(p, key + "reserved");

                if (reserved > onHand) {
                    throw new IllegalArgumentException(
                            "Reserved exceeds on-hand for "
                                    + product.getProductId()
                    );
                }

                if (products.putIfAbsent(
                        product.getProductId(),
                        new SavedProduct(product, onHand, reserved)
                ) != null) {
                    throw new IllegalArgumentException(
                            "Duplicate product ID"
                    );
                }
            }

            List<SavedOrder> orders = new ArrayList<>();
            Map<String, Long> fulfilledQuantities = new HashMap<>();

            int orderCount = count(p, "order.count", false);

            for (int i = 0; i < orderCount; i++) {

                String key = "order." + i + ".";

                String id = required(p, key + "id");
                OrderStatus status = OrderStatus.valueOf(
                        required(p, key + "status")
                );

                if (status == OrderStatus.CREATED) {
                    throw new IllegalArgumentException(
                            "Unreserved orders cannot be stored"
                    );
                }

                int itemCount = count(
                        p, key + "itemCount", true
                );

                Map<String, Integer> quantities =
                        new LinkedHashMap<>();

                for (int j = 0; j < itemCount; j++) {

                    String itemKey = key + "item." + j + ".";

                    String productId = required(
                            p, itemKey + "productId"
                    );

                    int quantity = positiveInt(
                            p, itemKey + "quantity"
                    );

                    BigDecimal unitPrice = new BigDecimal(
                            required(p, itemKey + "unitPrice")
                    );

                    SavedProduct product = products.get(productId);

                    if (product == null ||
                            unitPrice.compareTo(
                                    product.product().getPrice()
                            ) != 0) {
                        throw new IllegalArgumentException(
                                "Missing product or mismatched price: "
                                        + productId
                        );
                    }

                    if (quantities.putIfAbsent(
                            productId, quantity
                    ) != null) {
                        throw new IllegalArgumentException(
                                "Duplicate order item: " + productId
                        );
                    }

                    if (status == OrderStatus.FULFILLED) {
                        fulfilledQuantities.merge(
                                productId,
                                (long) quantity,
                                Math::addExact
                        );
                    }
                }

                orders.add(
                        new SavedOrder(id, status, quantities)
                );
            }

            // Step 1: Create inventory with a safe initial quantity.
            OrderService restored = new OrderService();

            for (SavedProduct saved : products.values()) {

                String productId = saved.product().getProductId();

                long requiredInitial = Math.addExact(
                        (long) saved.onHand(),
                        fulfilledQuantities.getOrDefault(productId, 0L)
                );

                int initialQuantity = (int) Math.min(
                        requiredInitial,
                        (long) Integer.MAX_VALUE
                );

                restored.registerProduct(
                        saved.product(),
                        initialQuantity
                );
            }

            // Step 2: Restore cancelled orders first.
            // They must not permanently occupy inventory.
            for (SavedOrder saved : orders) {

                if (saved.status() == OrderStatus.CANCELLED) {

                    restored.placeOrder(
                            saved.id(),
                            saved.quantities()
                    );

                    restored.cancelOrder(saved.id());
                }
            }

            // Step 3: Restore fulfilled orders.
            // Add temporary stock only when necessary.
            for (SavedOrder saved : orders) {

                if (saved.status() == OrderStatus.FULFILLED) {

                    for (Map.Entry<String, Integer> entry
                            : saved.quantities().entrySet()) {

                        String productId = entry.getKey();
                        int quantity = entry.getValue();

                        int available = restored
                                .getStock(productId)
                                .available();

                        if (quantity > available) {
                            restored.restockProduct(
                                    productId,
                                    quantity - available
                            );
                        }
                    }

                    restored.placeOrder(
                            saved.id(),
                            saved.quantities()
                    );

                    restored.fulfillOrder(saved.id());
                }
            }

            // Step 4: Restore the saved on-hand quantities.
            for (SavedProduct saved : products.values()) {

                String productId = saved.product().getProductId();

                int currentOnHand = restored
                        .getStock(productId)
                        .onHand();

                if (currentOnHand > saved.onHand()) {
                    throw new IllegalArgumentException(
                            "Inventory mismatch for " + productId
                    );
                }

                if (currentOnHand < saved.onHand()) {
                    restored.restockProduct(
                            productId,
                            saved.onHand() - currentOnHand
                    );
                }
            }

            // Step 5: Restore active reservations last.
            for (SavedOrder saved : orders) {

                if (saved.status() == OrderStatus.RESERVED) {

                    restored.placeOrder(
                            saved.id(),
                            saved.quantities()
                    );
                }
            }

            // Verify that the restored inventory matches the file.
            for (SavedProduct saved : products.values()) {

                InventorySnapshot actual = restored.getStock(
                        saved.product().getProductId()
                );

                if (actual.onHand() != saved.onHand()
                        || actual.reserved() != saved.reserved()) {

                    throw new IllegalArgumentException(
                            "Inventory mismatch for "
                                    + saved.product().getProductId()
                    );
                }
            }

            return restored;

        } catch (IllegalArgumentException |
                 IllegalStateException |
                 ArithmeticException e) {

            throw new IOException(
                    "Invalid application state: " + e.getMessage(), e
            );
        }
    }

    private static String required(Properties p, String key) {

        String value = p.getProperty(key);

        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing field: " + key
            );
        }

        return value;
    }

    private static int count(
            Properties p, String key, boolean mustBePositive
    ) {
        int number = Integer.parseInt(required(p, key));

        if (number < (mustBePositive ? 1 : 0)
                || number > 100_000) {

            throw new IllegalArgumentException(
                    "Invalid count: " + key
            );
        }

        return number;
    }

    private static int nonNegativeInt(Properties p, String key) {

        int number = Integer.parseInt(required(p, key));

        if (number < 0) {
            throw new IllegalArgumentException(
                    "Negative value: " + key
            );
        }

        return number;
    }

    private static int positiveInt(Properties p, String key) {

        int number = Integer.parseInt(required(p, key));

        if (number <= 0) {
            throw new IllegalArgumentException(
                    "Non-positive value: " + key
            );
        }

        return number;
    }

    private record SavedProduct(
            Product product,
            int onHand,
            int reserved
    ) {
    }

    private record SavedOrder(
            String id,
            OrderStatus status,
            Map<String, Integer> quantities
    ) {
    }
}
