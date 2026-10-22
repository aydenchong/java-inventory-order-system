package com.ayden.inventory;

import com.ayden.inventory.domain.Product;
import com.ayden.inventory.domain.Inventory;

import java.math.BigDecimal;

public class Main {

    public static void main(String[] args) {

        Product mouse = new Product(
                "P001", "Gaming Mouse", new BigDecimal("89.00")
        );

        Product keyboard = new Product(
                "P002", "Mechanical Keyboard", new BigDecimal("199.00")
        );

        Product cable = new Product(
                "P003", "USB-C Cable", new BigDecimal("29.00")
        );

        System.out.println("=== PRODUCT MANAGEMENT ===");

        displayProduct(mouse);
        displayProduct(keyboard);
        displayProduct(cable);

        try {
            new Product(
                    "P004", "Invalid Product", new BigDecimal("-10.00")
            );
            System.out.println("TEST FAILED!");
        } catch (IllegalArgumentException e) {
            System.out.println("Validation passed: " + e.getMessage());
        }

        System.out.println("\n=== INVENTORY MANAGEMENT ===");

        Inventory inventory = new Inventory(mouse.getProductId(), 20);

        System.out.println("\nInitial Inventory:");
        displayInventory(inventory);

        inventory.reserveStock(5);
        System.out.println("\nAfter Reserving 5:");
        displayInventory(inventory);

        inventory.fulfillStock(3);
        System.out.println("\nAfter Fulfilling 3:");
        displayInventory(inventory);

        inventory.releaseStock(2);
        System.out.println("\nAfter Releasing 2:");
        displayInventory(inventory);

        inventory.restock(10);
        System.out.println("\nAfter Restocking 10:");
        displayInventory(inventory);

        try {
            inventory.reserveStock(100);
            System.out.println("TEST FAILED!");
        } catch (IllegalStateException e) {
            System.out.println("\nValidation passed: " + e.getMessage());
        }
    }

    private static void displayProduct(Product product) {
        System.out.println("ID: " + product.getProductId());
        System.out.println("Name: " + product.getName());
        System.out.println("Price: RM" + product.getPrice());
        System.out.println();
    }

    private static void displayInventory(Inventory inventory) {
        System.out.println("Product ID: " + inventory.getProductId());
        System.out.println("On Hand: " + inventory.getQuantityOnHand());
        System.out.println("Reserved: " + inventory.getQuantityReserved());
        System.out.println("Available: " + inventory.getAvailableQuantity());
    }
}
