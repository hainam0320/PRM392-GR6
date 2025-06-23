package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.adapter.ProductAdapter;
import com.example.myapplication.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {
    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Shoe Store");

        progressBar = findViewById(R.id.progressBar);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);

        // Setup RecyclerView
        productsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(new ArrayList<>(), this);
        productsRecyclerView.setAdapter(productAdapter);

        // Load products
        loadProducts();
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("products")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        List<Product> products = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            product.setId(document.getId());
                            products.add(product);
                        }
                        productAdapter.updateProducts(products);
                    } else {
                        Toast.makeText(MainActivity.this, "Error loading products", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onProductClick(Product product) {
        // Handle product click - Navigate to product details
        Toast.makeText(this, "Selected: " + product.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddToCartClick(Product product) {
        // Handle add to cart
        Toast.makeText(this, "Added to cart: " + product.getName(), Toast.LENGTH_SHORT).show();
    }
}