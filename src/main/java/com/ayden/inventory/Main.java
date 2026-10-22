
package com.ayden.inventory;

import com.ayden.inventory.domain.Product;
import java.math.BigDecimal;

public class Main {

    public static void main(String[] args) {

        Product mouse = new Product(
                "P001",
                "Gaming Mouse",
                new BigDecimal("89.00")
        );

        Product keyboard = new Product(
                "P002",
                "Mechanical Keyboard",
                new BigDecimal("199.00")
        );

        Product cable = new Product(
                "P003",
                "USB-C Cable",
                new BigDecimal("29.00")
        );

        displayProduct(mouse);
        displayProduct(keyboard);
        displayProduct(cable);

        // Test invalid price
        try {
            Product invalid = new Product(
                    "P004",
                    "Invalid Product",
                    new BigDecimal("-10.00")
            );

            System.out.println("TEST FAILED: Invalid product was created!");

        } catch (IllegalArgumentException e) {
            System.out.println("Validation passed: " + e.getMessage());
        }
    }

    private static void displayProduct(Product product) {
        System.out.println("Product ID: " + product.getProductId());
        System.out.println("Name: " + product.getName());
        System.out.println("Price: RM" + product.getPrice());
        System.out.println();
    }
}
