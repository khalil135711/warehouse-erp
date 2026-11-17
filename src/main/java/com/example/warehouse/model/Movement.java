package com.example.warehouse.model;

public class Movement {
    private final long id;
    private final String sku;
    private final int delta;
    private final String type;
    private final String note;
    private final String createdAt;

    public Movement(long id, String sku, int delta, String type, String note, String createdAt) {
        this.id = id;
        this.sku = sku;
        this.delta = delta;
        this.type = type;
        this.note = note;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getSku() { return sku; }
    public int getDelta() { return delta; }
    public String getType() { return type; }
    public String getNote() { return note; }
    public String getCreatedAt() { return createdAt; }
}
