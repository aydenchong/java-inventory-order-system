package com.ayden.inventory.domain;

public record InventorySnapshot(
        int onHand,
        int reserved,
        int available
) {
}
