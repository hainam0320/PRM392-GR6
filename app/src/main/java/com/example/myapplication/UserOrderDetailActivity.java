package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.OrderDetailAdapter;
import com.example.myapplication.model.CartItem;
import com.example.myapplication.model.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import androidx.core.content.ContextCompat;
import com.example.myapplication.model.Product;
import java.util.Map;
import android.view.LayoutInflater;
import android.view.View;
import android.app.AlertDialog;
import android.widget.RatingBar;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputEditText;
import com.example.myapplication.model.Review;

public class UserOrderDetailActivity extends AppCompatActivity {
    private static final String TAG = "UserOrderDetail";
    private TextView textOrderId;
    private TextView textOrderDate;
    private TextView textOrderStatus;
    private TextView textPaymentMethod;
    private TextView textCustomerName;
    private TextView textCustomerPhone;
    private TextView textShippingAddress;
    private TextView textTotalAmount;
    private RecyclerView recyclerViewDetails;
    private OrderDetailAdapter detailAdapter;
    private FirebaseFirestore db;
    private String orderId;
    private List<CartItem> orderDetails;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;
    private Order order; // Added to store the order object for review check

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_order_detail);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        
        // Initialize formatters
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết đơn hàng");
        }

        // Initialize views
        initializeViews();

        // Get order ID from intent
        orderId = getIntent().getStringExtra("orderId");
        if (orderId != null) {
            loadOrderDetails(orderId);
        } else {
            Log.e(TAG, "Order ID is null");
            Toast.makeText(this, "Không thể tải thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        textOrderId = findViewById(R.id.textOrderId);
        textOrderDate = findViewById(R.id.textOrderDate);
        textOrderStatus = findViewById(R.id.textOrderStatus);
        textPaymentMethod = findViewById(R.id.textPaymentMethod);
        textCustomerName = findViewById(R.id.textCustomerName);
        textCustomerPhone = findViewById(R.id.textCustomerPhone);
        textShippingAddress = findViewById(R.id.textShippingAddress);
        textTotalAmount = findViewById(R.id.textTotalAmount);
        recyclerViewDetails = findViewById(R.id.recyclerViewDetails);

        // Setup RecyclerView
        orderDetails = new ArrayList<>();
        detailAdapter = new OrderDetailAdapter(orderDetails, this::showReviewDialog);
        recyclerViewDetails.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDetails.setAdapter(detailAdapter);
    }

    private void loadOrderDetails(String orderId) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        db.collection("orders")
            .document(orderId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!isFinishing()) {
                    Order order = documentSnapshot.toObject(Order.class);
                    if (order != null && order.getUserId().equals(userId)) {
                        order.setId(documentSnapshot.getId());
                        this.order = order; // Assign order to the class variable
                        displayOrderInfo(order);
                        loadOrderItems(orderId);
                    } else {
                        Log.e(TAG, "Order not found or unauthorized");
                        Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            })
            .addOnFailureListener(e -> {
                if (!isFinishing()) {
                    Log.e(TAG, "Error loading order details", e);
                    Toast.makeText(this, "Lỗi khi tải thông tin đơn hàng", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
    }

    private void displayOrderInfo(Order order) {
        try {
            if (order == null) {
                Log.e(TAG, "Order object is null");
                Toast.makeText(this, "Lỗi: Không có thông tin đơn hàng", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            textOrderId.setText("Mã đơn: " + (order.getId() != null ? order.getId() : ""));
            
            if (order.getOrderDate() > 0) {
                textOrderDate.setText("Ngày đặt: " + dateFormat.format(new Date(order.getOrderDate())));
            } else {
                textOrderDate.setText("Ngày đặt: Không xác định");
            }
            
            // Set status with appropriate color
            String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "pending";
            String statusText = getStatusText(status);
            textOrderStatus.setText(statusText);
            int statusColor = getStatusColor(status);
            textOrderStatus.setTextColor(ContextCompat.getColor(this, statusColor));

            String paymentMethod = order.getPaymentMethod();
            String paymentText = "BANK_TRANSFER".equals(paymentMethod) ? 
                               "Chuyển khoản ngân hàng" : "Thanh toán khi nhận hàng";
            textPaymentMethod.setText("Thanh toán: " + paymentText);
            
            textCustomerName.setText("Người nhận: " + (order.getCustomerName() != null ? order.getCustomerName() : ""));
            textCustomerPhone.setText("Số điện thoại: " + (order.getCustomerPhone() != null ? order.getCustomerPhone() : ""));
            textShippingAddress.setText("Địa chỉ: " + (order.getShippingAddress() != null ? order.getShippingAddress() : ""));
            
            double amount = order.getTotalAmount();
            String formattedAmount = amount > 0 ? currencyFormat.format(amount) : "0 ₫";
            textTotalAmount.setText("Tổng tiền: " + formattedAmount);
            
        } catch (Exception e) {
            Log.e(TAG, "Error displaying order info: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi hiển thị thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String getStatusText(String status) {
        switch (status.toLowerCase()) {
            case "pending": return "Chờ xử lý";
            case "waiting_payment": return "Chờ thanh toán";
            case "confirmed": return "Đã xác nhận";
            case "shipping": return "Đang giao hàng";
            case "completed": return "Hoàn thành";
            case "cancelled": return "Đã hủy";
            default: return status;
        }
    }

    private int getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "pending": return R.color.status_pending;
            case "waiting_payment": return R.color.status_waiting;
            case "confirmed": return R.color.status_confirmed;
            case "shipping": return R.color.status_shipping;
            case "completed": return R.color.status_completed;
            case "cancelled": return R.color.status_cancelled;
            default: return R.color.status_pending;
        }
    }

    private void loadOrderItems(String orderId) {
        db.collection("orders")
            .document(orderId)
            .collection("details")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!isFinishing()) {
                    try {
                        orderDetails.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            try {
                                // Get product details
                                Map<String, Object> data = doc.getData();
                                if (data != null) {
                                    Product product = new Product();
                                    // Safely get product data
                                    if (data.containsKey("product")) {
                                        Map<String, Object> productData = (Map<String, Object>) data.get("product");
                                        if (productData != null) {
                                            product.setId(productData.get("id") != null ? productData.get("id").toString() : "");
                                            product.setName(productData.get("name") != null ? productData.get("name").toString() : "");
                                            product.setPrice(productData.get("price") != null ? 
                                                Double.parseDouble(productData.get("price").toString()) : 0.0);
                                            
                                            // Get and set product images
                                            @SuppressWarnings("unchecked")
                                            List<String> images = (List<String>) productData.get("image");
                                            if (images != null && !images.isEmpty()) {
                                                product.setImage(images);
                                            }
                                        }
                                    }
                                    
                                    // Create CartItem
                                    CartItem item = new CartItem();
                                    item.setProduct(product);
                                    // Safely get quantity
                                    if (data.containsKey("quantity")) {
                                        Object quantityObj = data.get("quantity");
                                        if (quantityObj != null) {
                                            item.setQuantity(Integer.parseInt(quantityObj.toString()));
                                        } else {
                                            item.setQuantity(1);
                                        }
                                    } else {
                                        item.setQuantity(1);
                                    }
                                    
                                    orderDetails.add(item);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing order item: " + doc.getId(), e);
                                continue;
                            }
                        }
                        detailAdapter = new OrderDetailAdapter(orderDetails, this::showReviewDialog);
                        recyclerViewDetails.setAdapter(detailAdapter);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing order items", e);
                        Toast.makeText(UserOrderDetailActivity.this, 
                            "Có lỗi xảy ra khi tải chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .addOnFailureListener(e -> {
                if (!isFinishing()) {
                    Log.e(TAG, "Error loading order items", e);
                    Toast.makeText(this, "Lỗi khi tải chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showReviewDialog(Product product) {
        // Kiểm tra xem đơn hàng đã hoàn thành chưa
        if (!"completed".equalsIgnoreCase(order.getStatus())) {
            Toast.makeText(this, "Chỉ có thể đánh giá khi đơn hàng đã hoàn thành", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra xem đã đánh giá chưa
        db.collection("reviews")
            .whereEqualTo("productId", product.getId())
            .whereEqualTo("orderId", orderId)
            .whereEqualTo("userId", FirebaseAuth.getInstance().getCurrentUser().getUid())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    Toast.makeText(this, "Bạn đã đánh giá sản phẩm này", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Tạo dialog đánh giá
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                TextView productNameView = dialogView.findViewById(R.id.productName);
                RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
                TextInputEditText reviewInput = dialogView.findViewById(R.id.reviewInput);

                productNameView.setText(product.getName());

                builder.setView(dialogView)
                    .setTitle("Đánh giá sản phẩm")
                    .setPositiveButton("Gửi", (dialog, which) -> {
                        String content = reviewInput.getText().toString().trim();
                        float rating = ratingBar.getRating();

                        if (content.isEmpty()) {
                            Toast.makeText(this, "Vui lòng nhập nội dung đánh giá", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        submitReview(product, content, rating);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking existing review", e);
                Toast.makeText(this, "Lỗi khi kiểm tra đánh giá", Toast.LENGTH_SHORT).show();
            });
    }

    private void submitReview(Product product, String content, float rating) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Lấy username từ Firestore
        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                String username = documentSnapshot.getString("username");
                if (username == null) username = "Người dùng ẩn danh";

                Review review = new Review();
                review.setUserId(userId);
                review.setProductId(product.getId());
                review.setOrderId(orderId);
                review.setUsername(username);
                review.setContent(content);
                review.setRating(rating);

                // Lưu đánh giá vào Firestore
                db.collection("reviews")
                    .add(review)
                    .addOnSuccessListener(documentReference -> {
                        review.setId(documentReference.getId());
                        Toast.makeText(this, "Đã gửi đánh giá", Toast.LENGTH_SHORT).show();
                        
                        // Cập nhật rating trung bình của sản phẩm
                        updateProductRating(product.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error submitting review", e);
                        Toast.makeText(this, "Lỗi khi gửi đánh giá", Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting username", e);
                Toast.makeText(this, "Lỗi khi lấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            });
    }

    private void updateProductRating(String productId) {
        db.collection("reviews")
            .whereEqualTo("productId", productId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) return;

                float totalRating = 0;
                int count = 0;
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Review review = doc.toObject(Review.class);
                    if (review != null) {
                        totalRating += review.getRating();
                        count++;
                    }
                }

                float averageRating = totalRating / count;

                // Cập nhật rating trung bình vào sản phẩm
                db.collection("products").document(productId)
                    .update("averageRating", averageRating,
                            "reviewCount", count)
                    .addOnFailureListener(e -> 
                        Log.e(TAG, "Error updating product rating", e));
            })
            .addOnFailureListener(e -> 
                Log.e(TAG, "Error calculating average rating", e));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 