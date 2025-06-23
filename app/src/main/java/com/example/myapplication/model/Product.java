package com.example.myapplication.model;

import java.util.List;

public class Product {
    private String id;
    private String name;
    private double price;
    private int stock;
    private String brand;
    private String category;
    private String description;
    private List<String> image;

    public Product() {
        // Empty constructor needed for Firebase
    }

    public Product(String name, double price, int stock, String brand, String category, String description, List<String> image) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.brand = brand;
        this.category = category;
        this.description = description;
        this.image = image;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getImage() { return image; }
    public void setImage(List<String> image) { this.image = image; }
} 