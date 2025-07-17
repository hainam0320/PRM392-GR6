package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.adapter.OrderAdapter;
import com.example.myapplication.model.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class OrderListActivity extends AppCompatActivity implements OrderAdapter.OnOrderClickListener {
    private RecyclerView recyclerViewOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FirebaseFirestore db;
    private String currentUserId;

    private static final int ORDER_DETAIL_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý đơn hàng");
        }

        // Initialize views
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        // Setup RecyclerView
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList, this);
        recyclerViewOrders.setAdapter(orderAdapter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Check if user is admin
        checkUserRoleAndLoadOrders();
    }

    private void checkUserRoleAndLoadOrders() {
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String role = documentSnapshot.getString("role");
                    boolean isAdmin = "admin".equalsIgnoreCase(role);
                    loadOrders(isAdmin);
                } else {
                    loadOrders(false);
                }
            })
            .addOnFailureListener(e -> {
                loadOrders(false);
                Log.e("OrderList", "Error checking user role", e);
            });
    }

    private void loadOrders(boolean isAdmin) {
        progressBar.setVisibility(View.VISIBLE);
        
        Query query = db.collection("orders");
        if (!isAdmin) {
            // If not admin, only show user's orders
            query = query.whereEqualTo("userId", currentUserId);
        }
        
        query.get()
            .addOnSuccessListener(querySnapshot -> {
                progressBar.setVisibility(View.GONE);
                
                orderList.clear();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    try {
                        Order order = doc.toObject(Order.class);
                        if (order != null) {
                            order.setId(doc.getId());
                            orderList.add(order);
                        }
                    } catch (Exception e) {
                        Log.e("OrderParse", "Lỗi chuyển đổi document " + doc.getId() + ": " + e.getMessage());
                    }
                }

                // Sort the list in memory instead of in the query
                Collections.sort(orderList, (o1, o2) -> Long.compare(o2.getOrderDate(), o1.getOrderDate()));
                
                orderAdapter.updateOrders(orderList);
                updateOrderVisibility();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Log.e("OrderList", "Error loading orders", e);
                Toast.makeText(this, "Lỗi tải danh sách đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                updateOrderVisibility();
            });
    }

    private void updateOrderVisibility() {
        if (orderList.isEmpty()) {
            recyclerViewOrders.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("Không có đơn hàng nào");
        } else {
            recyclerViewOrders.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onOrderClick(Order order) {
        if (currentUserId != null) {
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        boolean isAdmin = "admin".equalsIgnoreCase(role);
                        
                        Intent detailIntent;
                        if (isAdmin) {
                            detailIntent = new Intent(this, OrderDetailActivity.class);
                            detailIntent.putExtra("orderId", order.getId());
                            startActivityForResult(detailIntent, ORDER_DETAIL_REQUEST);
                        } else {
                            detailIntent = new Intent(this, UserOrderDetailActivity.class);
                            detailIntent.putExtra("orderId", order.getId());
                            startActivity(detailIntent);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("OrderList", "Error checking user role", e);
                    Toast.makeText(this, "Lỗi: Không thể mở chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
                });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ORDER_DETAIL_REQUEST && resultCode == RESULT_OK) {
            if (data != null && data.getBooleanExtra("status_updated", false)) {
                // Reload the order list to show updated status
                checkUserRoleAndLoadOrders();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 