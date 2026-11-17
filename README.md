# Warehouse ERP (JavaFX)

A small desktop ERP for a warehouse, written in JavaFX with an embedded SQLite database.

## Features

- **Stock management**: products with SKU, name, location, quantity, minimum stock and price
  - search, add, edit, delete
  - book stock in / out with a note (every change is logged)
  - low-stock rows are highlighted, summary bar shows item count, units and stock value
- **Movement log**: full history of all stock bookings
- **Shipping labels**
  - fully customisable before printing: label size (presets or custom mm), font size, title,
    sender, recipient, address, reference, weight, contents, free note, logo, border
  - Code 39 barcode generated in pure Java (no extra library)
  - live preview while you type
  - save / load / delete your own label templates
  - print through the system print dialog, or export as high-resolution PNG
  - optional: pick a product + quantity and automatically book the stock out after printing

## Requirements

- JDK 17 or newer
- Maven 3.8+

## Run

```bash
mvn javafx:run
```

## Build

```bash
mvn package
```

## Data

The SQLite database is stored in `~/.warehouse-erp/warehouse.db` (created on first start).

## Printing tips

JavaFX prints with the paper size selected in the system print dialog. For a label printer
(Zebra, Brother, DYMO, DHL/DPD thermal printers, ...) choose the printer's label paper size
in the dialog and set the same size in the label settings. Labels larger than the printable
area are scaled down automatically.

## Project structure

```
src/main/java/com/example/warehouse
├── App.java                 entry point
├── db/                      SQLite access (Database, WarehouseRepository)
├── model/                   Product, Movement
├── label/                   LabelSpec, LabelRenderer, Code39
└── ui/                      Tabs and dialogs
```

## License

MIT
