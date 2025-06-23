package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

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

public class AdminMainActivity extends AppCompatActivity implements AdminProductAdapter.OnProductClickListener {
    private RecyclerView recyclerView;
    private AdminProductAdapter productAdapter;
    private ArrayList<Product> productList;
    private FirebaseFirestore db;
    private FloatingActionButton fabAdd;
    private Button buttonLogout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        recyclerView = findViewById(R.id.recyclerViewProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        productAdapter = new AdminProductAdapter(productList, this);
        recyclerView.setAdapter(productAdapter);

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
    }

    private void loadProducts() {
        db.collection("products")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + error, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    productList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            Product p = doc.toObject(Product.class);
                            if (p != null) {
                                p.setId(doc.getId());
                                productList.add(p);
                            }
                        } catch (Exception e) {
                            Log.e("ProductParse", "Lỗi chuyển đổi document " + doc.getId() + ": " + e.getMessage());
                        }
                    }
                    productAdapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, EditProductActivity.class);
        intent.putExtra("productId", product.getId());
        startActivity(intent);
    }
}
