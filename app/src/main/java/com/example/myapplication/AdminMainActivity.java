// AdminMainActivity.java
package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.ProductAdapter;
import com.example.myapplication.model.Product;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class AdminMainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private ArrayList<Product> productList;
    private FirebaseFirestore db;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        recyclerView = findViewById(R.id.recyclerViewProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        productAdapter = new ProductAdapter(this, productList);
        recyclerView.setAdapter(productAdapter);

        db = FirebaseFirestore.getInstance();
        loadProducts();

        fabAdd = findViewById(R.id.fabAddProduct);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(AdminMainActivity.this, EditProductActivity.class);
            startActivity(intent);
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
}
