package com.example.myapplication;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RatingBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import com.example.myapplication.adapter.ImageSliderAdapter;
import com.example.myapplication.model.Product;
import com.example.myapplication.model.CartItem;
import com.example.myapplication.model.Review;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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
    private RatingBar productRating;
    private TextView ratingValue;
    private TextView reviewCount;
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
        productRating = findViewById(R.id.productRating);
        ratingValue = findViewById(R.id.ratingValue);
        reviewCount = findViewById(R.id.reviewCount);

        // Get product ID from intent
        String productId = getIntent().getStringExtra("product_id");
        if (productId != null) {
            loadProductDetails(productId);
            loadReviews(productId);
        } else {
            Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadReviews(String productId) {
        // First, load the average rating
        db.collection("reviews")
            .whereEqualTo("productId", productId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                float totalRating = 0;
                int count = 0;

                for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                    try {
                        Review review = document.toObject(Review.class);
                        if (review != null) {
                            totalRating += review.getRating();
                            count++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Update rating display
                if (count > 0) {
                    float averageRating = totalRating / count;
                    productRating.setRating(averageRating);
                    ratingValue.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
                    reviewCount.setText(String.format(Locale.getDefault(), "(%d đánh giá)", count));
                } else {
                    productRating.setRating(0);
                    ratingValue.setText("0.0");
                    reviewCount.setText("(0 đánh giá)");
                }
            })
            .addOnFailureListener(e -> {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi tải đánh giá", Toast.LENGTH_SHORT).show();
                // Set default values in case of error
                productRating.setRating(0);
                ratingValue.setText("0.0");
                reviewCount.setText("(0 đánh giá)");
            });
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

        // Disable button and update UI if product is out of stock
        if (product.getStock() <= 0) {
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Hết hàng");
            Toast.makeText(this, "Sản phẩm hiện đã hết hàng", Toast.LENGTH_SHORT).show();
        } else {
            btnAddToCart.setEnabled(true);
            btnAddToCart.setText("Thêm vào giỏ hàng");
        }

        btnAddToCart.setOnClickListener(v -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                return;
            }

            // Double check stock before adding to cart
            if (product.getStock() <= 0) {
                Toast.makeText(this, "Sản phẩm đã hết hàng", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = auth.getCurrentUser().getUid();
            CartItem cartItem = new CartItem(product, 1);

            // Check current cart for existing items
            db.collection("users").document(userId)
                .collection("cart")
                .whereEqualTo("product.id", product.getId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Product already exists in cart
                        CartItem existingItem = querySnapshot.getDocuments().get(0).toObject(CartItem.class);
                        if (existingItem != null) {
                            int newQuantity = existingItem.getQuantity() + 1;
                            if (newQuantity > product.getStock()) {
                                Toast.makeText(ProductDetailActivity.this, 
                                    "Không thể thêm vào giỏ hàng. Số lượng vượt quá hàng có sẵn.", 
                                    Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // Update existing cart item quantity
                            querySnapshot.getDocuments().get(0).getReference()
                                .update("quantity", newQuantity)
                                .addOnSuccessListener(aVoid -> 
                                    Toast.makeText(ProductDetailActivity.this, 
                                        "Đã cập nhật số lượng trong giỏ hàng", 
                                        Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> 
                                    Toast.makeText(ProductDetailActivity.this, 
                                        "Lỗi khi cập nhật giỏ hàng", 
                                        Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        // Add new item to cart
                        db.collection("users").document(userId)
                            .collection("cart")
                            .add(cartItem)
                            .addOnSuccessListener(documentReference -> 
                                Toast.makeText(ProductDetailActivity.this, 
                                    "Đã thêm " + product.getName() + " vào giỏ hàng", 
                                    Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> 
                                Toast.makeText(ProductDetailActivity.this, 
                                    "Lỗi khi thêm vào giỏ hàng", 
                                    Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(ProductDetailActivity.this, 
                        "Lỗi khi kiểm tra giỏ hàng", 
                        Toast.LENGTH_SHORT).show());
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