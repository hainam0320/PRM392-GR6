package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
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

public class FavoritesActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {
    private static final String TAG = "FavoritesActivity";
    private RecyclerView favoritesRecyclerView;
    private ProductAdapter productAdapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sản phẩm yêu thích");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView);

        // Setup RecyclerView
        favoritesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(new ArrayList<>(), this);
        favoritesRecyclerView.setAdapter(productAdapter);

        // Load favorites
        loadFavorites();
    }

    private void loadFavorites() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem sản phẩm yêu thích", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);
        favoritesRecyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        db.collection("users")
            .document(userId)
            .collection("favorites")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                progressBar.setVisibility(View.GONE);
                List<String> favoriteProductIds = new ArrayList<>();
                
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String productId = doc.getString("productId");
                    if (productId != null) {
                        favoriteProductIds.add(productId);
                    }
                }

                if (favoriteProductIds.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    favoritesRecyclerView.setVisibility(View.GONE);
                } else {
                    loadFavoriteProducts(favoriteProductIds);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading favorites", e);
                progressBar.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Lỗi khi tải danh sách yêu thích", Toast.LENGTH_SHORT).show();
            });
    }

    private void loadFavoriteProducts(List<String> productIds) {
        List<Product> favoriteProducts = new ArrayList<>();
        int[] loadedCount = {0};

        for (String productId : productIds) {
            db.collection("products")
                .document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Product product = documentSnapshot.toObject(Product.class);
                    if (product != null) {
                        product.setId(documentSnapshot.getId());
                        favoriteProducts.add(product);
                    }
                    loadedCount[0]++;

                    if (loadedCount[0] == productIds.size()) {
                        if (favoriteProducts.isEmpty()) {
                            emptyView.setVisibility(View.VISIBLE);
                            favoritesRecyclerView.setVisibility(View.GONE);
                        } else {
                            emptyView.setVisibility(View.GONE);
                            favoritesRecyclerView.setVisibility(View.VISIBLE);
                            productAdapter.updateProducts(favoriteProducts);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading product: " + productId, e);
                    loadedCount[0]++;
                    if (loadedCount[0] == productIds.size()) {
                        if (favoriteProducts.isEmpty()) {
                            emptyView.setVisibility(View.VISIBLE);
                            favoritesRecyclerView.setVisibility(View.GONE);
                        }
                    }
                });
        }
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

        // Check if product is out of stock
        if (product.getStock() <= 0) {
            Toast.makeText(this, "Sản phẩm " + product.getName() + " đã hết hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        CartItem cartItem = new CartItem(product, 1);

        db.collection("users")
            .document(userId)
            .collection("cart")
            .add(cartItem)
            .addOnSuccessListener(documentReference -> 
                Toast.makeText(this, "Đã thêm vào giỏ hàng: " + product.getName(), Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding to cart", e);
                Toast.makeText(this, "Lỗi khi thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites(); // Reload favorites when returning to this screen
    }
} 