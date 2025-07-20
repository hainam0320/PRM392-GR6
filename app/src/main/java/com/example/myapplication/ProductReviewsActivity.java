package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.adapter.ReviewAdapter;
import com.example.myapplication.model.Review;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductReviewsActivity extends AppCompatActivity {
    private TextView productName;
    private RatingBar productRating;
    private TextView ratingValue;
    private TextView reviewCount;
    private RecyclerView reviewsRecyclerView;
    private ReviewAdapter reviewAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_reviews);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Đánh giá sản phẩm");
        }

        // Initialize views
        productName = findViewById(R.id.productName);
        productRating = findViewById(R.id.productRating);
        ratingValue = findViewById(R.id.ratingValue);
        reviewCount = findViewById(R.id.reviewCount);
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);

        // Setup RecyclerView
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(new ArrayList<>());
        reviewsRecyclerView.setAdapter(reviewAdapter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get product data from intent
        String productId = getIntent().getStringExtra("product_id");
        String name = getIntent().getStringExtra("product_name");
        
        if (productId != null && name != null) {
            productName.setText(name);
            loadReviews(productId);
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadReviews(String productId) {
        db.collection("reviews")
            .whereEqualTo("productId", productId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                float totalRating = 0;
                int count = 0;
                List<Review> reviews = new ArrayList<>();

                for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                    try {
                        Review review = document.toObject(Review.class);
                        if (review != null) {
                            review.setId(document.getId());
                            reviews.add(review);
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

                // Sort reviews by timestamp (newest first)
                reviews.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                
                // Update reviews list
                reviewAdapter.updateReviews(reviews);
            })
            .addOnFailureListener(e -> {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi tải đánh giá", Toast.LENGTH_SHORT).show();
                productRating.setRating(0);
                ratingValue.setText("0.0");
                reviewCount.setText("(0 đánh giá)");
            });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 