package com.example.myapplication.model;

public class CartItem {
    private String id;
    private Product product;
    private int quantity;

    public CartItem() {
        // Empty constructor needed for Firebase
    }

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getTotalPrice() {
        return product.getPrice() * quantity;
    }
} 