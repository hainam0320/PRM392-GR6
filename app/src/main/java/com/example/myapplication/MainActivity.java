package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.adapter.ProductAdapter;
import com.example.myapplication.model.Product;
import com.example.myapplication.model.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {
    private static final String TAG = "MainActivity";
    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Shoe Store");
        }

        progressBar = findViewById(R.id.progressBar);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);

        // Setup RecyclerView
        productsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(new ArrayList<>(), this);
        productsRecyclerView.setAdapter(productAdapter);

        // Load products
        loadProducts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            auth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_cart) {
            startActivity(new Intent(this, CartActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_edit_profile) {
            startActivity(new Intent(this, EditProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        productsRecyclerView.setVisibility(View.GONE);

        db.collection("products")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    productsRecyclerView.setVisibility(View.VISIBLE);
                    
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Product> products = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Product product = document.toObject(Product.class);
                                product.setId(document.getId());
                                products.add(product);
                            } catch (Exception e) {
                                Log.e(TAG, "Error converting document " + document.getId(), e);
                            }
                        }
                        productAdapter.updateProducts(products);
                        
                        if (products.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Không tìm thấy sản phẩm nào", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Error getting products", task.getException());
                        Toast.makeText(MainActivity.this, "Lỗi khi tải sản phẩm", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
    }

    @Override
    public void onAddToCartClick(Product product) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        CartItem cartItem = new CartItem(product, 1);

        db.collection("users").document(userId)
                .collection("cart")
                .add(cartItem)
                .addOnSuccessListener(documentReference -> 
                    Toast.makeText(this, "Đã thêm vào giỏ hàng: " + product.getName(), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Lỗi khi thêm vào giỏ hàng", Toast.LENGTH_SHORT).show());
    }
}