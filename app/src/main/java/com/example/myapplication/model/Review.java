package com.example.myapplication.model;

public class Review {
    private String id;
    private String userId;
    private String productId;
    private String orderId;
    private String username;
    private String content;
    private float rating;
    private long timestamp;

    public Review() {
        // Empty constructor needed for Firestore
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getProductId() { return productId; }
    public String getOrderId() { return orderId; }
    public String getUsername() { return username; }
    public String getContent() { return content; }
    public float getRating() { return rating; }
    public long getTimestamp() { return timestamp; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setProductId(String productId) { this.productId = productId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setUsername(String username) { this.username = username; }
    public void setContent(String content) { this.content = content; }
    public void setRating(float rating) { this.rating = rating; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
} 