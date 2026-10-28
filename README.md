# Java Inventory & Order Management System

[![Java CI](https://github.com/aydenchong/java-inventory-order-system/actions/workflows/java-ci.yml/badge.svg)](https://github.com/aydenchong/java-inventory-order-system/actions/workflows/java-ci.yml)

A **standalone Java 21 command-line application** for managing product inventory and multi-item orders. It demonstrates object-oriented domain modeling, stock reservations, order state transitions, defensive validation, file persistence, CSV reporting, and automated testing—without Spring Boot or a database.

## Why this project?

An order-management system must do more than store products and orders. It must enforce business rules: customers cannot reserve more stock than is available; canceling an order must release the correct quantity; and fulfilling an order must deduct physical stock exactly once.

This project focuses on these rules using **Core Java**, in-memory collections, and file-based persistence.

## Features

- **Product registration** with validated IDs, names, and positive prices (`BigDecimal`).
- **Inventory tracking** with on-hand, reserved, and available quantities.
- **Restocking** with validation against negative values and integer overflow.
- **Multi-item order placement** that validates all requested items before reserving stock.
- **Order fulfillment and cancellation** with guarded state transitions.
- **Interactive CLI** to perform inventory and order operations.
- **CSV inventory export** for reporting.
- **Save/load application state** using a versioned `.properties` file, including orders and reservations.
- **JUnit 5 tests** covering domain validation, order workflows, regression scenarios, and persistence.

## Technology

| Component | Technology |
| --- | --- |
| Language | Java 21 |
| Build | Apache Maven |
| Tests | JUnit 5, Maven Surefire |
| Interface | Console CLI (`Scanner`) |
| Storage | In-memory `HashMap` + local file persistence (`Properties`, `java.nio.file`) |
| Reports | CSV |
| CI | GitHub Actions |

## Domain model and responsibilities

| Class | Responsibility |
| --- | --- |
| `Product` | Immutable product identity, name, and price |
| `Inventory` | Maintains on-hand and reserved quantities and validates stock operations |
| `OrderItem` | Captures ordered quantity and unit price at order creation |
| `Order` / `OrderStatus` | Holds order items, calculates totals, and guards lifecycle transitions |
| `OrderService` | Coordinates product registration, reservations, fulfillment, cancellation, and restocking |
| `InventorySnapshot` | Exposes a read-only snapshot of stock quantities |
| `InventoryCsvExporter` | Writes current inventory to a CSV report |
| `ApplicationStateStore` | Saves and restores validated application state |
| `Main` | Handles console input and menu navigation |

### Inventory rules

```text
Available = On Hand - Reserved
0 <= Reserved <= On Hand
```

- **Reserve:** increase `Reserved`; physical stock remains unchanged.
- **Fulfill:** decrease both `On Hand` and `Reserved`.
- **Cancel:** decrease `Reserved`; do not deduct physical stock.
- **Restock:** increase `On Hand` without changing existing reservations.

The order lifecycle is:

```text
CREATED -> RESERVED -> FULFILLED
                    -> CANCELLED
```

The application prevents repeated fulfillment/cancellation of a completed order. Before placing a multi-item order, it validates every item to avoid partial reservations caused by ordinary validation failures in its **single-threaded** execution model.

## Project structure

```text
src/
├── main/java/com/ayden/inventory/
│   ├── Main.java
│   ├── domain/
│   │   ├── Product.java
│   │   ├── Inventory.java
│   │   ├── InventorySnapshot.java
│   │   ├── OrderItem.java
│   │   ├── OrderStatus.java
│   │   ├── Order.java
│   │   └── OrderService.java
│   └── io/
│       ├── InventoryCsvExporter.java
│       └── ApplicationStateStore.java
└── test/java/com/ayden/inventory/
    ├── domain/             # Domain and order-processing tests
    └── io/                 # CSV and persistence tests
```

## Getting started

### Requirements

- JDK 21
- Apache Maven 3.9+ (or another Maven version compatible with the project's plugins)

### Clone and build

```bash
git clone https://github.com/aydenchong/java-inventory-order-system.git
cd java-inventory-order-system
mvn clean verify
```

### Run the application

In IntelliJ IDEA, open the Maven project and run `com.ayden.inventory.Main`.

Alternatively, after compiling, run from the repository root:

```bash
mvn compile
java -cp target/classes com.ayden.inventory.Main
```

The application shows an interactive menu for registering products, viewing stock, placing orders, fulfilling or canceling orders, exporting CSV, restocking, and saving/loading data.

## Example workflow

1. Register product `P001` (Gaming Mouse, RM89.00) with **20 on hand**.
2. Place `ORD001` for **3 × P001**. The order becomes `RESERVED`.
3. Observe **20 on hand, 3 reserved, 17 available**.
4. Fulfill `ORD001`. The order becomes `FULFILLED`.
5. Observe **17 on hand, 0 reserved, 17 available**.

A canceled `RESERVED` order releases its reserved stock instead of reducing on-hand stock.

## Data storage and reporting

- **Application state:** `data/app-state.properties` (loaded on startup when present; saved by the menu or during normal exit).
- **Inventory report:** `reports/inventory.csv`.
- These are generated local files and should be excluded from Git with `.gitignore`.

The state store validates the saved version, product/order data, and restored inventory quantities. A damaged or incompatible state file is rejected rather than silently loaded.

**Important:** CSV is a report, not a full application backup. The `.properties` state file is used for recovery.

## Testing

Run all tests locally:

```bash
mvn clean verify
```

The test suite covers normal and invalid product/inventory operations, overselling prevention, order status rules, multi-order regressions, CSV export, and state restoration. GitHub Actions runs the Maven verification command on pushes and pull requests to `main`.

## Design decisions and limitations

- **Pure Java:** No Spring Boot, REST API, database, or GUI; the goal is to demonstrate Java fundamentals and business rules.
- **Single-threaded:** Reservation validation is not concurrency-safe and is not equivalent to a database transaction.
- **Local storage:** The app is not a distributed service. Unsaved changes may be lost if the process is forcibly terminated.
- **Current product model:** Product prices are immutable; the state-reconstruction approach assumes the current product price matches the recorded order-item price.
- **Scope:** This is a learning and portfolio project, not a production-grade inventory system.

## Engineering takeaways

The project is designed to show how Java collections, encapsulation, state transitions, exceptions, and automated tests work together to protect business invariants. It also illustrates trade-offs between simple file persistence and database-backed transactional systems.
