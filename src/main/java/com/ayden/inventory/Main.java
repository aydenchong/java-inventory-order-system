package com.ayden.inventory;

import com.ayden.inventory.domain.*;
import com.ayden.inventory.io.ApplicationStateStore;
import com.ayden.inventory.io.InventoryCsvExporter;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    private static final Path SAVE_FILE =
            Path.of("data", "app-state.properties");

    public static void main(String[] args) {

        OrderService service = new OrderService();

        // Automatically load saved data on startup.
        if (Files.exists(SAVE_FILE)) {
            try {
                service = ApplicationStateStore.load(SAVE_FILE);
                System.out.println("Previous application state restored!");

            } catch (IOException e) {
                System.out.println("Unable to load saved data: "
                        + e.getMessage());
                System.out.println(
                        "Application stopped to protect the existing save file."
                );
                scanner.close();
                return;
            }
        }

        boolean running = true;

        while (running) {

            displayMenu();
            String choice = readInput("Select an option: ");

            try {
                switch (choice) {

                    case "1" -> registerProduct(service);

                    case "2" -> viewStock(service);

                    case "3" -> placeOrder(service);

                    case "4" -> fulfillOrder(service);

                    case "5" -> cancelOrder(service);

                    case "6" -> viewOrder(service);

                    case "7" -> exportInventoryReport(service);

                    case "8" -> saveData(service);

                    case "9" -> {
                        String confirmation = readInput(
                                "Discard unsaved changes and reload? (yes/no): "
                        );

                        if (confirmation.equalsIgnoreCase("yes")) {
                            service = ApplicationStateStore.load(SAVE_FILE);
                            System.out.println("Data reloaded successfully!");
                        } else {
                            System.out.println("Reload cancelled.");
                        }
                    }

                    case "10" -> restockProduct(service);

                    case "0" -> {
                        ApplicationStateStore.save(service, SAVE_FILE);
                        running = false;
                        System.out.println("Data saved. Goodbye!");
                    }

                    default -> System.out.println("Invalid menu option!");
                }

            } catch (IllegalArgumentException |
                     IllegalStateException |
                     IOException e) {

                System.out.println("Operation failed: " + e.getMessage());
            }

            System.out.println();
        }

        scanner.close();
    }

    private static void displayMenu() {
        System.out.println("=====================================");
        System.out.println("  INVENTORY & ORDER MANAGEMENT");
        System.out.println("=====================================");
        System.out.println("1. Register Product");
        System.out.println("2. View Product Stock");
        System.out.println("3. Place Order");
        System.out.println("4. Fulfill Order");
        System.out.println("5. Cancel Order");
        System.out.println("6. View Order");
        System.out.println("7. Export Inventory CSV");
        System.out.println("8. Save Data");
        System.out.println("9. Load Data");
        System.out.println("10. Restock Product");
        System.out.println("0. Exit");
        System.out.println("=====================================");
    }

    private static String readInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static int readInt(String prompt) {
        return Integer.parseInt(readInput(prompt));
    }

    private static void registerProduct(OrderService service) {

        System.out.println("\n--- Register Product ---");

        String id = readInput("Product ID: ");
        String name = readInput("Product Name: ");
        BigDecimal price = new BigDecimal(readInput("Price (RM): "));
        int quantity = readInt("Initial Stock: ");

        Product product = new Product(id, name, price);
        service.registerProduct(product, quantity);

        System.out.println("Product registered successfully!");
    }

    private static void viewStock(OrderService service) {

        System.out.println("\n--- View Stock ---");

        String id = readInput("Product ID: ");
        InventorySnapshot stock = service.getStock(id);

        System.out.println("On Hand: " + stock.onHand());
        System.out.println("Reserved: " + stock.reserved());
        System.out.println("Available: " + stock.available());
    }

    private static void restockProduct(OrderService service) {

        System.out.println("\n--- Restock Product ---");

        String productId = readInput("Product ID: ");
        int quantity = readInt("Quantity to add: ");

        service.restockProduct(productId, quantity);

        InventorySnapshot stock = service.getStock(productId);

        System.out.println("Product restocked successfully!");
        System.out.println("On Hand: " + stock.onHand());
        System.out.println("Reserved: " + stock.reserved());
        System.out.println("Available: " + stock.available());
    }

    private static void placeOrder(OrderService service) {

        System.out.println("\n--- Place Order ---");

        String orderId = readInput("Order ID: ");
        int count = readInt("Number of different products: ");

        if (count <= 0) {
            throw new IllegalArgumentException(
                    "Order must contain at least one product!"
            );
        }

        Map<String, Integer> quantities = new LinkedHashMap<>();

        for (int i = 0; i < count; i++) {

            System.out.println("\nItem " + (i + 1));

            String productId = readInput("Product ID: ");
            int quantity = readInt("Quantity: ");

            if (quantities.putIfAbsent(productId, quantity) != null) {
                throw new IllegalArgumentException(
                        "Duplicate product in order: " + productId
                );
            }
        }

        Order order = service.placeOrder(orderId, quantities);

        System.out.println("Order placed successfully!");
        displayOrder(order);
    }

    private static void fulfillOrder(OrderService service) {

        System.out.println("\n--- Fulfill Order ---");

        String orderId = readInput("Order ID: ");
        service.fulfillOrder(orderId);

        System.out.println("Order fulfilled successfully!");
    }

    private static void cancelOrder(OrderService service) {

        System.out.println("\n--- Cancel Order ---");

        String orderId = readInput("Order ID: ");
        service.cancelOrder(orderId);

        System.out.println("Order cancelled successfully!");
    }

    private static void viewOrder(OrderService service) {

        System.out.println("\n--- View Order ---");

        String orderId = readInput("Order ID: ");
        Order order = service.getOrder(orderId);

        displayOrder(order);
    }

    private static void exportInventoryReport(OrderService service) {

        Path outputFile = Path.of("reports", "inventory.csv");

        try {
            int count = InventoryCsvExporter.export(service, outputFile);

            System.out.println("Exported " + count + " products to:");
            System.out.println(outputFile.toAbsolutePath());

        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private static void saveData(OrderService service)
            throws IOException {

        ApplicationStateStore.save(service, SAVE_FILE);

        System.out.println("Data saved successfully!");
        System.out.println(SAVE_FILE.toAbsolutePath());
    }

    private static void displayOrder(Order order) {

        System.out.println("\nOrder ID: " + order.getOrderId());
        System.out.println("Status: " + order.getStatus());

        for (OrderItem item : order.getItems()) {

            System.out.println(
                    "Product: " + item.getProductId()
                            + " | Quantity: " + item.getQuantity()
                            + " | Subtotal: RM" + item.getSubtotal()
            );
        }

        System.out.println("Total: RM" + order.getTotalAmount());
    }
}
