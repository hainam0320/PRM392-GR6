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
        detailAdapter = new OrderDetailAdapter(orderDetails);
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
                                            // Add other product fields as needed
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
                                // Continue with next item instead of breaking
                                continue;
                            }
                        }
                        detailAdapter.notifyDataSetChanged();
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 