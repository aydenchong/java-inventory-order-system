package com.ayden.inventory.domain;

import java.math.BigDecimal;

public class Product {
    private final String productId;
    private final String name;
    private final BigDecimal price;

    public Product(String productId, String name, BigDecimal price){
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Invalid Product ID!");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Invalid Name!");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be greater than zero!");
        }

        this.productId = productId;
        this.name = name;
        this.price = price;
    }

    public String getProductId(){
        return productId;
    }

    public String getName(){
        return name;
    }

    public BigDecimal getPrice(){
        return price;
    }
}
