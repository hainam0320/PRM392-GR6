package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.myapplication.model.Order;
import com.example.myapplication.model.CartItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {
    private TextView textOrderId, textCustomerName, textCustomerPhone, textShippingAddress, textOrderDate, textPaymentMethod, textTotalAmount;
    private Spinner spinnerStatus;
    private Button btnSaveStatus;
    private RecyclerView recyclerViewDetails;
    private FirebaseFirestore db;
    private String orderId;
    private Order order;
    private List<CartItem> orderDetails = new ArrayList<>();
    private OrderDetailAdapter detailAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        textOrderId = findViewById(R.id.textOrderId);
        textCustomerName = findViewById(R.id.textCustomerName);
        textCustomerPhone = findViewById(R.id.textCustomerPhone);
        textShippingAddress = findViewById(R.id.textShippingAddress);
        textOrderDate = findViewById(R.id.textOrderDate);
        textPaymentMethod = findViewById(R.id.textPaymentMethod);
        textTotalAmount = findViewById(R.id.textTotalAmount);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnSaveStatus = findViewById(R.id.btnSaveStatus);
        recyclerViewDetails = findViewById(R.id.recyclerViewDetails);
        recyclerViewDetails.setLayoutManager(new LinearLayoutManager(this));
        detailAdapter = new OrderDetailAdapter(orderDetails);
        recyclerViewDetails.setAdapter(detailAdapter);

        db = FirebaseFirestore.getInstance();
        orderId = getIntent().getStringExtra("orderId");
        if (orderId != null) {
            loadOrderDetail(orderId);
        }

        btnSaveStatus.setOnClickListener(v -> saveStatus());
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadOrderDetail(String orderId) {
        db.collection("orders").document(orderId).get().addOnSuccessListener(documentSnapshot -> {
            order = documentSnapshot.toObject(Order.class);
            if (order != null) {
                order.setId(documentSnapshot.getId());
                displayOrderInfo(order);
                loadOrderItems(orderId);
            }
        });
    }

    private void displayOrderInfo(Order order) {
        textOrderId.setText("Mã đơn: " + order.getId());
        textCustomerName.setText("Khách: " + order.getCustomerName());
        textCustomerPhone.setText("SĐT: " + order.getCustomerPhone());
        textShippingAddress.setText("Địa chỉ: " + order.getShippingAddress());
        textOrderDate.setText("Ngày: " + order.getOrderDate());
        textPaymentMethod.setText("Thanh toán: " + order.getPaymentMethod());
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        textTotalAmount.setText("Tổng: " + format.format(order.getTotalAmount()));
        // Setup spinner
        String[] statusArr = {"Pending", "Shipping", "Completed", "Cancelled"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusArr);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
        for (int i = 0; i < statusArr.length; i++) {
            if (statusArr[i].equalsIgnoreCase(order.getStatus())) {
                spinnerStatus.setSelection(i);
                break;
            }
        }
    }

    private void loadOrderItems(String orderId) {
        db.collection("orders").document(orderId).collection("details").get().addOnSuccessListener(queryDocumentSnapshots -> {
            orderDetails.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                CartItem item = doc.toObject(CartItem.class);
                orderDetails.add(item);
            }
            detailAdapter.notifyDataSetChanged();
        });
    }

    private void saveStatus() {
        String newStatus = spinnerStatus.getSelectedItem().toString();
        if (order != null && !newStatus.equals(order.getStatus())) {
            DocumentReference orderRef = db.collection("orders").document(orderId);
            orderRef.update("status", newStatus).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
                order.setStatus(newStatus);
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi cập nhật trạng thái", Toast.LENGTH_SHORT).show();
            });
        }
    }
} 