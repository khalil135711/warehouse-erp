package com.example.warehouse.model;

public class Product {
    private final long id;
    private final String sku;
    private final String name;
    private final String location;
    private final int quantity;
    private final int minStock;
    private final double price;

    public Product(long id, String sku, String name, String location, int quantity, int minStock, double price) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.location = location;
        this.quantity = quantity;
        this.minStock = minStock;
        this.price = price;
    }

    public long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public int getQuantity() { return quantity; }
    public int getMinStock() { return minStock; }
    public double getPrice() { return price; }
}
