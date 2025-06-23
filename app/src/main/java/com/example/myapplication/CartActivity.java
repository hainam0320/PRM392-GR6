package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.CartAdapter;
import com.example.myapplication.model.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartItemClickListener {
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private List<CartItem> cartItems;
    private TextView totalPriceText;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Giỏ hàng");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize views
        recyclerView = findViewById(R.id.cart_recycler_view);
        totalPriceText = findViewById(R.id.total_price);
        Button checkoutButton = findViewById(R.id.checkout_button);

        // Setup RecyclerView
        cartItems = new ArrayList<>();
        adapter = new CartAdapter(cartItems, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load cart items
        loadCartItems();

        // Setup checkout button
        checkoutButton.setOnClickListener(v -> processCheckout());
    }

    private void loadCartItems() {
        db.collection("users").document(userId)
                .collection("cart")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi khi tải giỏ hàng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    cartItems.clear();
                    for (QueryDocumentSnapshot document : value) {
                        CartItem item = document.toObject(CartItem.class);
                        item.setId(document.getId());
                        cartItems.add(item);
                    }
                    adapter.updateData(cartItems);
                    updateTotalPrice();
                });
    }

    private void updateTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        totalPriceText.setText(formatter.format(total));
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        db.collection("users").document(userId)
                .collection("cart").document(item.getId())
                .update("quantity", newQuantity)
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi cập nhật số lượng", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDeleteItem(CartItem item) {
        db.collection("users").document(userId)
                .collection("cart").document(item.getId())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã xóa sản phẩm khỏi giỏ hàng", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi xóa sản phẩm", Toast.LENGTH_SHORT).show());
    }

    private void processCheckout() {
        // TODO: Implement checkout process
        Toast.makeText(this, "Tính năng thanh toán đang được phát triển", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 