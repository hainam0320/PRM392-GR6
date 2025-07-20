package com.example.myapplication.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import android.widget.RatingBar;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> products;
    private OnProductClickListener listener;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onAddToCartClick(Product product);
    }

    public ProductAdapter(List<Product> products, OnProductClickListener listener) {
        this.products = products;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product, listener);
        
        // Check if product is in favorites
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            db.collection("users")
                .document(userId)
                .collection("favorites")
                .whereEqualTo("productId", product.getId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean isFavorite = !querySnapshot.isEmpty();
                    holder.updateFavoriteButton(isFavorite);
                })
                .addOnFailureListener(e -> {
                    Log.e("ProductAdapter", "Error checking favorite status", e);
                });
        }
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    public void updateProducts(List<Product> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private ImageView productImage;
        private TextView productName;
        private TextView productBrand;
        private TextView productPrice;
        private Button btnAddToCart;
        private ImageButton btnFavorite;
        private RatingBar productRating;
        private TextView ratingValue;
        private boolean isFavorite = false;

        ProductViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            productBrand = itemView.findViewById(R.id.productBrand);
            productPrice = itemView.findViewById(R.id.productPrice);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            productRating = itemView.findViewById(R.id.productRating);
            ratingValue = itemView.findViewById(R.id.ratingValue);
        }

        void updateFavoriteButton(boolean favorite) {
            isFavorite = favorite;
            btnFavorite.setImageResource(favorite ? 
                R.drawable.ic_favorite_filled : 
                R.drawable.ic_favorite_border);
        }

        void bind(final Product product, final OnProductClickListener listener) {
            productName.setText(product.getName());
            productBrand.setText(product.getBrand());
            
            // Format price to VND currency
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            productPrice.setText(format.format(product.getPrice()));

            // Set rating
            float rating = product.getAverageRating();
            productRating.setRating(rating);
            ratingValue.setText(String.format("%.1f", rating));

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

            // Check if product is out of stock
            if (product.getStock() <= 0) {
                btnAddToCart.setEnabled(false);
                btnAddToCart.setText("HẾT HÀNG");
                btnAddToCart.setAlpha(0.5f);
            } else {
                btnAddToCart.setEnabled(true);
                btnAddToCart.setText("THÊM VÀO GIỎ HÀNG");
                btnAddToCart.setAlpha(1.0f);
            }

            itemView.setOnClickListener(v -> listener.onProductClick(product));
            btnAddToCart.setOnClickListener(v -> {
                if (product.getStock() <= 0) {
                    Toast.makeText(v.getContext(), 
                        "Sản phẩm " + product.getName() + " đã hết hàng", 
                        Toast.LENGTH_SHORT).show();
                } else {
                    listener.onAddToCartClick(product);
                }
            });

            btnFavorite.setOnClickListener(v -> {
                if (auth.getCurrentUser() == null) {
                    Toast.makeText(v.getContext(), 
                        "Vui lòng đăng nhập để thêm vào yêu thích", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                String userId = auth.getCurrentUser().getUid();
                Log.d("ProductAdapter", "Product ID: " + product.getId());
                Log.d("ProductAdapter", "User ID: " + userId);

                if (isFavorite) {
                    // Remove from favorites
                    db.collection("users")
                        .document(userId)
                        .collection("favorites")
                        .whereEqualTo("productId", product.getId())
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            Log.d("ProductAdapter", "Found " + querySnapshot.size() + " favorites to remove");
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                doc.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        updateFavoriteButton(false);
                                        Toast.makeText(v.getContext(), 
                                            "Đã xóa khỏi danh sách yêu thích", 
                                            Toast.LENGTH_SHORT).show();
                                        Log.d("ProductAdapter", "Successfully removed favorite");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("ProductAdapter", "Error removing favorite", e);
                                        Toast.makeText(v.getContext(), 
                                            "Lỗi khi xóa khỏi yêu thích", 
                                            Toast.LENGTH_SHORT).show();
                                    });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ProductAdapter", "Error querying favorites", e);
                            Toast.makeText(v.getContext(), 
                                "Lỗi khi truy cập danh sách yêu thích", 
                                Toast.LENGTH_SHORT).show();
                        });
                } else {
                    // Add to favorites
                    Map<String, Object> favorite = new HashMap<>();
                    favorite.put("productId", product.getId());
                    favorite.put("timestamp", System.currentTimeMillis());

                    Log.d("ProductAdapter", "Adding favorite with data: " + favorite);

                    db.collection("users")
                        .document(userId)
                        .collection("favorites")
                        .add(favorite)
                        .addOnSuccessListener(documentReference -> {
                            updateFavoriteButton(true);
                            Toast.makeText(v.getContext(), 
                                "Đã thêm vào danh sách yêu thích", 
                                Toast.LENGTH_SHORT).show();
                            Log.d("ProductAdapter", "Successfully added favorite with ID: " + documentReference.getId());
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ProductAdapter", "Error adding favorite", e);
                            Toast.makeText(v.getContext(), 
                                "Lỗi khi thêm vào yêu thích", 
                                Toast.LENGTH_SHORT).show();
                        });
                }
            });
        }
    }
} 