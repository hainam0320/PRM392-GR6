package com.example.myapplication.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RatingBar;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.ProductReviewsActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.ProductViewHolder> {
    private List<Product> products;
    private OnProductClickListener listener;
    private FirebaseFirestore db;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public AdminProductAdapter(List<Product> products, OnProductClickListener listener) {
        this.products = products;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_admin, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product, listener);
        loadProductRating(product.getId(), holder);
    }

    private void loadProductRating(String productId, ProductViewHolder holder) {
        db.collection("reviews")
            .whereEqualTo("productId", productId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                float totalRating = 0;
                int count = querySnapshot.size();
                
                for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                    totalRating += document.getDouble("rating").floatValue();
                }

                if (count > 0) {
                    float averageRating = totalRating / count;
                    holder.productRating.setRating(averageRating);
                    holder.reviewCount.setText(String.format(Locale.getDefault(), "(%d)", count));
                } else {
                    holder.productRating.setRating(0);
                    holder.reviewCount.setText("(0)");
                }
            })
            .addOnFailureListener(e -> {
                holder.productRating.setRating(0);
                holder.reviewCount.setText("(0)");
            });
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    public void updateProducts(List<Product> newProducts) {
        this.products.clear();
        this.products.addAll(newProducts);
        notifyDataSetChanged();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private ImageView productImage;
        private TextView productName;
        private TextView productBrand;
        private TextView productPrice;
        private RatingBar productRating;
        private TextView reviewCount;
        private Button btnViewReviews;

        ProductViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            productBrand = itemView.findViewById(R.id.productBrand);
            productPrice = itemView.findViewById(R.id.productPrice);
            productRating = itemView.findViewById(R.id.productRating);
            reviewCount = itemView.findViewById(R.id.reviewCount);
            btnViewReviews = itemView.findViewById(R.id.btnViewReviews);
        }

        void bind(final Product product, final OnProductClickListener listener) {
            productName.setText(product.getName());
            productBrand.setText(product.getBrand());
            
            // Format price to VND currency
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            productPrice.setText(format.format(product.getPrice()));

            // Load the first image from the list if available
            if (product.getImage() != null && !product.getImage().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(product.getImage().get(0))
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(productImage);
            } else {
                productImage.setImageResource(R.drawable.ic_launcher_background);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });

            // Set up view reviews button
            btnViewReviews.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), ProductReviewsActivity.class);
                intent.putExtra("product_id", product.getId());
                intent.putExtra("product_name", product.getName());
                itemView.getContext().startActivity(intent);
            });
        }
    }
} 