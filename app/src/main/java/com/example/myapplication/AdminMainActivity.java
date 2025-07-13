package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.AdminProductAdapter;
import com.example.myapplication.model.Product;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminMainActivity extends AppCompatActivity implements AdminProductAdapter.OnProductClickListener {
    private RecyclerView recyclerView;
    private AdminProductAdapter productAdapter;
    private ArrayList<Product> productList;
    private ArrayList<Product> allProducts;
    private FirebaseFirestore db;
    private FloatingActionButton fabAdd;
    private Button buttonLogout;
    private EditText searchEditText;
    private TextView emptyView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        recyclerView = findViewById(R.id.recyclerViewProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        allProducts = new ArrayList<>();
        productAdapter = new AdminProductAdapter(productList, this);
        recyclerView.setAdapter(productAdapter);
        emptyView = findViewById(R.id.emptyView);

        db = FirebaseFirestore.getInstance();
        loadProducts();

        fabAdd = findViewById(R.id.fabAddProduct);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(AdminMainActivity.this, EditProductActivity.class);
            startActivity(intent);
        });

        // Xử lý nút Đăng xuất
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(AdminMainActivity.this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminMainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateProductVisibility() {
        if (productAdapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void filterProducts(String text) {
        ArrayList<Product> filteredList = new ArrayList<>();
        for (Product product : allProducts) {
            if (product.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(product);
            }
        }
        productAdapter.updateProducts(filteredList);
        updateProductVisibility();
    }

    private void loadProducts() {
        db.collection("products")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + error, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allProducts.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            Product p = doc.toObject(Product.class);
                            if (p != null) {
                                p.setId(doc.getId());
                                allProducts.add(p);
                            }
                        } catch (Exception e) {
                            Log.e("ProductParse", "Lỗi chuyển đổi document " + doc.getId() + ": " + e.getMessage());
                        }
                    }
                    productAdapter.updateProducts(allProducts);
                    updateProductVisibility();
                    // Also update the local productList if needed elsewhere, though filtering should use allProducts
                    productList.clear();
                    productList.addAll(allProducts);
                });
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, EditProductActivity.class);
        intent.putExtra("productId", product.getId());
        startActivity(intent);
    }
}
