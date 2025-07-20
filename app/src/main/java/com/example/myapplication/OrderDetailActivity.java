package com.example.myapplication;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.myapplication.model.Order;
import com.example.myapplication.model.CartItem;
import com.example.myapplication.model.Product;
import com.example.myapplication.adapter.OrderDetailAdapter;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.util.Map;
import android.util.Log;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import android.content.Intent;

public class OrderDetailActivity extends AppCompatActivity {
    private TextView textOrderId, textCustomerName, textCustomerPhone, textShippingAddress, textOrderDate, textPaymentMethod, textTotalAmount;
    private TextView textOrderStatus;
    private Spinner spinnerStatus;
    private Button btnSaveStatus;
    private RecyclerView recyclerViewDetails;
    private FirebaseFirestore db;
    private String orderId;
    private Order order;
    private List<CartItem> orderDetails = new ArrayList<>();
    private OrderDetailAdapter detailAdapter;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        // Initialize formatters
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết đơn hàng");
        }

        initializeViews();

        db = FirebaseFirestore.getInstance();
        orderId = getIntent().getStringExtra("orderId");
        if (orderId != null) {
            loadOrderDetail(orderId);
        } else {
            Toast.makeText(this, "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSaveStatus.setOnClickListener(v -> saveStatus());
    }

    private void initializeViews() {
        textOrderId = findViewById(R.id.textOrderId);
        textCustomerName = findViewById(R.id.textCustomerName);
        textCustomerPhone = findViewById(R.id.textCustomerPhone);
        textShippingAddress = findViewById(R.id.textShippingAddress);
        textOrderDate = findViewById(R.id.textOrderDate);
        textPaymentMethod = findViewById(R.id.textPaymentMethod);
        textTotalAmount = findViewById(R.id.textTotalAmount);
        textOrderStatus = findViewById(R.id.textOrderStatus);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnSaveStatus = findViewById(R.id.btnSaveStatus);
        recyclerViewDetails = findViewById(R.id.recyclerViewDetails);

        recyclerViewDetails.setLayoutManager(new LinearLayoutManager(this));
        // Pass null as review listener for admin view
        detailAdapter = new OrderDetailAdapter(orderDetails, null);
        recyclerViewDetails.setAdapter(detailAdapter);

        // Setup spinner
        String[] statusArr = {"Pending", "Waiting_Payment", "Confirmed", "Shipping", "Completed", "Cancelled"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusArr);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
    }

    private void loadOrderDetail(String orderId) {
        db.collection("orders").document(orderId).get()
            .addOnSuccessListener(documentSnapshot -> {
                order = documentSnapshot.toObject(Order.class);
                if (order != null) {
                    order.setId(documentSnapshot.getId());
                    displayOrderInfo(order);
                    loadOrderItems(orderId);
                } else {
                    Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi khi tải thông tin đơn hàng", Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void displayOrderInfo(Order order) {
        try {
            textOrderId.setText("Mã đơn: " + order.getId());
            textCustomerName.setText("Khách hàng: " + order.getCustomerName());
            textCustomerPhone.setText("SĐT: " + order.getCustomerPhone());
            textShippingAddress.setText("Địa chỉ: " + order.getShippingAddress());
            
            if (order.getOrderDate() > 0) {
                textOrderDate.setText("Ngày đặt: " + dateFormat.format(new Date(order.getOrderDate())));
            } else {
                textOrderDate.setText("Ngày đặt: Không xác định");
            }

            String paymentText = order.getPaymentMethod().equals("BANK_TRANSFER") ? 
                               "Chuyển khoản ngân hàng" : "Thanh toán khi nhận hàng";
            textPaymentMethod.setText("Thanh toán: " + paymentText);
            
            textTotalAmount.setText("Tổng tiền: " + currencyFormat.format(order.getTotalAmount()));

            // Set spinner selection
            String currentStatus = order.getStatus();
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerStatus.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).equalsIgnoreCase(currentStatus)) {
                    spinnerStatus.setSelection(i);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e("OrderDetail", "Error displaying order info", e);
            Toast.makeText(this, "Lỗi hiển thị thông tin đơn hàng", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadOrderItems(String orderId) {
        db.collection("orders")
            .document(orderId)
            .collection("details")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
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
                            Log.e("OrderDetail", "Error parsing order item: " + doc.getId(), e);
                            continue;
                        }
                    }
                    detailAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Log.e("OrderDetail", "Error processing order items", e);
                    Toast.makeText(this, "Có lỗi xảy ra khi tải chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("OrderDetail", "Error loading order items", e);
                Toast.makeText(this, "Lỗi khi tải chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
            });
    }

    private void saveStatus() {
        if (order != null) {
            String newStatus = spinnerStatus.getSelectedItem().toString();
            if (!newStatus.equals(order.getStatus())) {
                // Show loading indicator
                btnSaveStatus.setEnabled(false);
                spinnerStatus.setEnabled(false);

                db.collection("orders")
                    .document(orderId)
                    .update("status", newStatus)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã cập nhật trạng thái đơn hàng", Toast.LENGTH_SHORT).show();
                        
                        // Set result to indicate data was changed
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("status_updated", true);
                        setResult(RESULT_OK, resultIntent);
                        
                        // Finish this activity to return to order list
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi khi cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                        Log.e("OrderDetail", "Error updating status", e);
                        
                        // Reset spinner to previous status
                        String currentStatus = order.getStatus();
                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerStatus.getAdapter();
                        for (int i = 0; i < adapter.getCount(); i++) {
                            if (adapter.getItem(i).equalsIgnoreCase(currentStatus)) {
                                spinnerStatus.setSelection(i);
                                break;
                            }
                        }
                    })
                    .addOnCompleteListener(task -> {
                        // Re-enable controls
                        btnSaveStatus.setEnabled(true);
                        spinnerStatus.setEnabled(true);
                    });
            }
        }
    }

    private void updateStatusUI(String status) {
        // Update spinner selection
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerStatus.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equalsIgnoreCase(status)) {
                spinnerStatus.setSelection(i);
                break;
            }
        }

        // Update status text color if needed
        int statusColor = getStatusColor(status);
        if (textOrderStatus != null) {  // Fixed: Changed from textOrderDate to textOrderStatus
            textOrderStatus.setText(getStatusText(status));
            textOrderStatus.setTextColor(ContextCompat.getColor(this, statusColor));
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

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 