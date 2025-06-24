package com.example.myapplication;

import android.content.Intent;
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
    private static final int SHIPPING_INFO_REQUEST = 1001;
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private List<CartItem> cartItems;
    private TextView totalPriceText;
    private FirebaseFirestore db;
    private String userId;
    private double totalAmount = 0;

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Giỏ hàng");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

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
        checkoutButton.setOnClickListener(v -> proceedToPayment());
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
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            CartItem item = document.toObject(CartItem.class);
                            item.setId(document.getId());
                            cartItems.add(item);
                        }
                        adapter.updateData(cartItems);
                        updateTotalPrice();
                    }
                });
    }

    private void updateTotalPrice() {
        totalAmount = 0;
        for (CartItem item : cartItems) {
            totalAmount += item.getTotalPrice();
        }
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        totalPriceText.setText(formatter.format(totalAmount));
    }

    private void proceedToPayment() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo danh sách ID sản phẩm và số lượng
        ArrayList<String> productIds = new ArrayList<>();
        ArrayList<Integer> quantities = new ArrayList<>();
        for (CartItem item : cartItems) {
            productIds.add(item.getProduct().getId());
            quantities.add(item.getQuantity());
        }

        Intent intent = new Intent(this, ShippingInfoActivity.class);
        intent.putStringArrayListExtra("product_ids", productIds);
        intent.putIntegerArrayListExtra("quantities", quantities);
        intent.putExtra("total_amount", totalAmount);
        startActivityForResult(intent, SHIPPING_INFO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SHIPPING_INFO_REQUEST && resultCode == RESULT_OK) {
            // Thanh toán thành công, chuyển về MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // Đóng CartActivity
        }
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        if (newQuantity <= 0) {
            onDeleteItem(item);
            return;
        }
        
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 