// EditProductActivity.java
package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.model.Product;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EditProductActivity extends AppCompatActivity {

    private EditText editName, editPrice, editStock, editBrand, editCategory, editDescription, editImageUrl;
    private MaterialButton btnSave, btnDelete;
    private FirebaseFirestore db;
    private String productId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        editName = findViewById(R.id.editProductName);
        editPrice = findViewById(R.id.editProductPrice);
        editStock = findViewById(R.id.editProductStock);
        editBrand = findViewById(R.id.editProductBrand);
        editCategory = findViewById(R.id.editProductCategory);
        editDescription = findViewById(R.id.editProductDescription);
        editImageUrl = findViewById(R.id.editProductImageUrl);
        btnSave = findViewById(R.id.buttonSaveProduct);
        btnDelete = findViewById(R.id.buttonDeleteProduct);

        db = FirebaseFirestore.getInstance();

        productId = getIntent().getStringExtra("productId");
        if (productId != null) {
            // Load product data from Firestore
            db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Product product = documentSnapshot.toObject(Product.class);
                        if (product != null) {
                            editName.setText(product.getName());
                            editPrice.setText(String.valueOf(product.getPrice()));
                            editStock.setText(String.valueOf(product.getStock()));
                            editBrand.setText(product.getBrand());
                            editCategory.setText(product.getCategory());
                            editDescription.setText(product.getDescription());
                            if (product.getImage() != null && !product.getImage().isEmpty()) {
                                editImageUrl.setText(product.getImage().get(0));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            
            // Hiển thị nút xóa chỉ khi đang chỉnh sửa sản phẩm
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            // Ẩn nút xóa khi đang thêm sản phẩm mới
            btnDelete.setVisibility(View.GONE);
        }

        btnSave.setOnClickListener(v -> saveProduct());

        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa sản phẩm này?")
            .setPositiveButton("Xóa", (dialog, which) -> deleteProduct())
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void deleteProduct() {
        if (productId != null) {
            db.collection("products").document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
        }
    }

    private void saveProduct() {
        String name = editName.getText().toString().trim();
        String priceStr = editPrice.getText().toString().trim();
        String stockStr = editStock.getText().toString().trim();
        String brand = editBrand.getText().toString().trim();
        String category = editCategory.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String imageUrl = editImageUrl.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(stockStr)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse numbers safely
        double price;
        int stock;
        try {
            price = Double.parseDouble(priceStr);
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giá và số lượng phải là số hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate numbers
        if (price < 0 || stock < 0) {
            Toast.makeText(this, "Giá và số lượng không thể âm", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> imageList = new ArrayList<>();
        if (!TextUtils.isEmpty(imageUrl)) {
            imageList.add(imageUrl);
        }

        if (productId != null) {
            db.collection("products").document(productId)
                    .update("name", name,
                            "price", price,
                            "stock", stock,
                            "brand", brand,
                            "category", category,
                            "description", description,
                            "image", imageList)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Product product = new Product(name, price, stock, brand, category, description, imageList);
            db.collection("products").add(product)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Thêm thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}
