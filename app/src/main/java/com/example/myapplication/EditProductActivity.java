// EditProductActivity.java
package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EditProductActivity extends AppCompatActivity {

    private EditText editName, editPrice, editStock, editBrand, editCategory, editDescription, editImageUrl;
    private Button btnSave;
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

        db = FirebaseFirestore.getInstance();

        productId = getIntent().getStringExtra("productId");
        if (productId != null) {
            editName.setText(getIntent().getStringExtra("name"));
            editPrice.setText(String.valueOf(getIntent().getDoubleExtra("price", 0)));
            editStock.setText(String.valueOf(getIntent().getIntExtra("stock", 0)));
            editBrand.setText(getIntent().getStringExtra("brand"));
            editCategory.setText(getIntent().getStringExtra("category"));
            editDescription.setText(getIntent().getStringExtra("description"));
            String imageUrl = getIntent().getStringExtra("image");
            if (imageUrl != null) {
                editImageUrl.setText(imageUrl);
            }
        }

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString();
            double price = Double.parseDouble(editPrice.getText().toString());
            int stock = Integer.parseInt(editStock.getText().toString());
            String brand = editBrand.getText().toString();
            String category = editCategory.getText().toString();
            String description = editDescription.getText().toString();
            String imageUrl = editImageUrl.getText().toString();
            List<String> imageList = new ArrayList<>();
            imageList.add(imageUrl);

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
        });
    }
}
