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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class OrderListActivity extends AppCompatActivity implements OrderAdapter.OnOrderClickListener {
    private RecyclerView recyclerViewOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;
    private List<Order> allOrders;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        orderList = new ArrayList<>();
        allOrders = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList, this);
        recyclerViewOrders.setAdapter(orderAdapter);
        db = FirebaseFirestore.getInstance();
        loadOrders();
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("orders").addSnapshotListener((value, error) -> {
            progressBar.setVisibility(View.GONE);
            if (error != null || value == null) {
                Toast.makeText(this, "Lỗi tải đơn hàng: " + error, Toast.LENGTH_SHORT).show();
                return;
            }
            allOrders.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                try {
                    Order order = doc.toObject(Order.class);
                    if (order != null) {
                        order.setId(doc.getId());
                        allOrders.add(order);
                    }
                } catch (Exception e) {
                    Log.e("OrderParse", "Lỗi chuyển đổi document " + doc.getId() + ": " + e.getMessage());
                }
            }
            orderAdapter.updateOrders(allOrders);
            updateOrderVisibility();
            orderList.clear();
            orderList.addAll(allOrders);
        });
    }

    private void updateOrderVisibility() {
        if (orderAdapter.getItemCount() == 0) {
            recyclerViewOrders.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerViewOrders.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onOrderClick(Order order) {
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra("orderId", order.getId());
        startActivity(intent);
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