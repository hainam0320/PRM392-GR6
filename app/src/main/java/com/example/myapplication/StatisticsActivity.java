package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.myapplication.model.User;
import com.example.myapplication.model.Order;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {
    private TextView textTotalUsers, textTotalRevenue;
    private RecyclerView recyclerViewUserRevenue;
    private UserRevenueAdapter userRevenueAdapter;
    private FirebaseFirestore db;
    private List<User> userList = new ArrayList<>();
    private List<Order> orderList = new ArrayList<>();
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        textTotalUsers = findViewById(R.id.textTotalUsers);
        textTotalRevenue = findViewById(R.id.textTotalRevenue);
        recyclerViewUserRevenue = findViewById(R.id.recyclerViewUserRevenue);
        recyclerViewUserRevenue.setLayoutManager(new LinearLayoutManager(this));
        userRevenueAdapter = new UserRevenueAdapter(new ArrayList<>());
        recyclerViewUserRevenue.setAdapter(userRevenueAdapter);

        db = FirebaseFirestore.getInstance();
        loadStatistics();
    }

    private void loadStatistics() {
        db.collection("users").get().addOnSuccessListener(userSnapshot -> {
            userList.clear();
            for (DocumentSnapshot doc : userSnapshot.getDocuments()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    user.setId(doc.getId());
                    userList.add(user);
                }
            }
            textTotalUsers.setText("Tổng số khách hàng: " + userList.size());
            loadOrders();
        }).addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải người dùng", Toast.LENGTH_SHORT).show());
    }

    private void loadOrders() {
        db.collection("orders").get().addOnSuccessListener(orderSnapshot -> {
            orderList.clear();
            for (DocumentSnapshot doc : orderSnapshot.getDocuments()) {
                Order order = doc.toObject(Order.class);
                if (order != null) {
                    order.setId(doc.getId());
                    orderList.add(order);
                }
            }
            double totalRevenue = 0;
            Map<String, Double> userRevenueMap = new HashMap<>();
            for (Order order : orderList) {
                totalRevenue += order.getTotalAmount();
                String userId = order.getUserId();
                userRevenueMap.put(userId, userRevenueMap.getOrDefault(userId, 0.0) + order.getTotalAmount());
            }
            textTotalRevenue.setText("Tổng chi tiêu: " + currencyFormat.format(totalRevenue));
            // Chuẩn bị dữ liệu cho adapter
            List<UserRevenue> userRevenueList = new ArrayList<>();
            for (User user : userList) {
                double revenue = userRevenueMap.getOrDefault(user.getId(), 0.0);
                userRevenueList.add(new UserRevenue(user.getUsername() != null ? user.getUsername() : user.getEmail(), revenue));
            }
            userRevenueAdapter.updateData(userRevenueList);
        }).addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải đơn hàng", Toast.LENGTH_SHORT).show());
    }

    // Model cho adapter
    public static class UserRevenue {
        public String name;
        public double revenue;
        public UserRevenue(String name, double revenue) {
            this.name = name;
            this.revenue = revenue;
        }
    }

    // Adapter cho danh sách chi tiêu từng khách hàng
    public static class UserRevenueAdapter extends RecyclerView.Adapter<UserRevenueAdapter.ViewHolder> {
        private List<UserRevenue> data;
        private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        public UserRevenueAdapter(List<UserRevenue> data) { this.data = data; }
        public void updateData(List<UserRevenue> newData) {
            this.data = newData;
            notifyDataSetChanged();
        }
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            UserRevenue item = data.get(position);
            holder.text1.setText(item.name);
            holder.text2.setText(currencyFormat.format(item.revenue));
        }
        @Override
        public int getItemCount() { return data != null ? data.size() : 0; }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(android.view.View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
} 