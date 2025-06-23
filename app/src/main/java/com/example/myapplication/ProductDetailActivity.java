package com.example.myapplication;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import com.example.myapplication.adapter.ImageSliderAdapter;
import com.example.myapplication.model.Product;
import com.example.myapplication.model.CartItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {
    private ViewPager2 imageSlider;
    private TabLayout imageIndicator;
    private TextView productName;
    private TextView productBrand;
    private TextView productPrice;
    private TextView productDescription;
    private TextView productCategory;
    private TextView productStock;
    private MaterialButton btnAddToCart;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        imageSlider = findViewById(R.id.imageSlider);
        imageIndicator = findViewById(R.id.imageIndicator);
        productName = findViewById(R.id.productName);
        productBrand = findViewById(R.id.productBrand);
        productPrice = findViewById(R.id.productPrice);
        productDescription = findViewById(R.id.productDescription);
        productCategory = findViewById(R.id.productCategory);
        productStock = findViewById(R.id.productStock);
        btnAddToCart = findViewById(R.id.btnAddToCart);

        // Get product ID from intent
        String productId = getIntent().getStringExtra("product_id");
        if (productId != null) {
            loadProductDetails(productId);
        } else {
            Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadProductDetails(String productId) {
        db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Product product = documentSnapshot.toObject(Product.class);
                    if (product != null) {
                        displayProductDetails(product);
                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi tải thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayProductDetails(Product product) {
        // Set up image slider
        ImageSliderAdapter imageAdapter = new ImageSliderAdapter(product.getImage());
        imageSlider.setAdapter(imageAdapter);

        // Set up image indicator
        new TabLayoutMediator(imageIndicator, imageSlider,
                (tab, position) -> {
                    // Empty implementation, dots are styled through tab_selector.xml
                }).attach();

        // Set product details
        productName.setText(product.getName());
        productBrand.setText(product.getBrand());
        
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        productPrice.setText(format.format(product.getPrice()));
        
        productDescription.setText(product.getDescription());
        productCategory.setText(product.getCategory());
        productStock.setText(String.valueOf(product.getStock()));

        btnAddToCart.setOnClickListener(v -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
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
                        Toast.makeText(this, "Đã thêm " + product.getName() + " vào giỏ hàng", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> 
                        Toast.makeText(this, "Lỗi khi thêm vào giỏ hàng", Toast.LENGTH_SHORT).show());
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
} 