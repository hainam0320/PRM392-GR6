package com.example.myapplication;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.model.Product;
import com.example.myapplication.model.Review;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import com.bumptech.glide.Glide;
import android.widget.LinearLayout;
import android.view.ViewGroup.LayoutParams;
import android.view.Gravity;
import androidx.appcompat.widget.Toolbar;

public class ProductPerformanceActivity extends AppCompatActivity {
    private RecyclerView recyclerViewProductPerformance;
    private ProductPerformanceAdapter productPerformanceAdapter;
    private FirebaseFirestore db;
    private RecyclerView recyclerViewTopBest;
    private RecyclerView recyclerViewTopRated;
    private ProductPerformanceAdapter adapterBest;
    private ProductPerformanceAdapter adapterRated;
    private LinearLayout sectionLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_performance);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Báo cáo hiệu suất");
        }

        recyclerViewTopBest = findViewById(R.id.recyclerViewTopBest);
        recyclerViewTopBest.setLayoutManager(new LinearLayoutManager(this));
        adapterBest = new ProductPerformanceAdapter(new ArrayList<>(), false);
        recyclerViewTopBest.setAdapter(adapterBest);
        db = FirebaseFirestore.getInstance();
        loadProductPerformance();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadProductPerformance() {
        db.collection("products").get().addOnSuccessListener(productSnapshot -> {
            List<Product> products = new ArrayList<>();
            for (DocumentSnapshot doc : productSnapshot.getDocuments()) {
                String id = doc.getId();
                String name = doc.getString("name");
                Double price = doc.getDouble("price");
                Long stock = doc.getLong("stock");
                String brand = doc.getString("brand");
                String category = doc.getString("category");
                String description = doc.getString("description");
                Object imageObj = doc.get("image");
                List<String> imageList = new ArrayList<>();
                if (imageObj instanceof List) {
                    for (Object o : (List<?>) imageObj) {
                        if (o instanceof String) imageList.add((String) o);
                    }
                } else if (imageObj instanceof String) {
                    imageList.add((String) imageObj);
                }
                Product product = new Product(
                    name,
                    price != null ? price : 0,
                    stock != null ? stock.intValue() : 0,
                    brand,
                    category,
                    description,
                    imageList
                );
                product.setId(id);
                products.add(product);
            }
            if (products.isEmpty()) {
                adapterBest.updateData(new ArrayList<>());
                return;
            }
            db.collection("orders").get().addOnSuccessListener(orderSnapshot -> {
                Map<String, Integer> productPurchaseCount = new HashMap<>();
                List<com.google.android.gms.tasks.Task<?>> detailTasks = new ArrayList<>();
                for (DocumentSnapshot orderDoc : orderSnapshot.getDocuments()) {
                    detailTasks.add(db.collection("orders").document(orderDoc.getId()).collection("details").get()
                        .addOnSuccessListener(detailsSnapshot -> {
                            for (DocumentSnapshot detailDoc : detailsSnapshot.getDocuments()) {
                                Map<String, Object> data = detailDoc.getData();
                                if (data != null && data.containsKey("product")) {
                                    Map<String, Object> productData = (Map<String, Object>) data.get("product");
                                    if (productData != null && productData.containsKey("id")) {
                                        String pid = productData.get("id").toString();
                                        int qty = 1;
                                        if (data.containsKey("quantity")) {
                                            Object q = data.get("quantity");
                                            try { qty = Integer.parseInt(q.toString()); } catch (Exception ignore) {}
                                        }
                                        productPurchaseCount.put(pid, productPurchaseCount.getOrDefault(pid, 0) + qty);
                                    }
                                }
                            }
                        }));
                }
                com.google.android.gms.tasks.Tasks.whenAllSuccess(detailTasks).addOnSuccessListener(results -> {
                    db.collection("reviews").get().addOnSuccessListener(reviewSnapshot -> {
                        Map<String, int[]> productStarCount = new HashMap<>();
                        for (QueryDocumentSnapshot reviewDoc : reviewSnapshot) {
                            Review review = reviewDoc.toObject(Review.class);
                            if (review != null && review.getProductId() != null) {
                                int[] arr = productStarCount.getOrDefault(review.getProductId(), new int[5]);
                                int star = (int) review.getRating();
                                if (star >= 1 && star <= 5) arr[star-1]++;
                                productStarCount.put(review.getProductId(), arr);
                            }
                        }
                        List<ProductPerformance> perfList = new ArrayList<>();
                        for (Product p : products) {
                            int purchase = productPurchaseCount.getOrDefault(p.getId(), 0);
                            int[] stars = productStarCount.getOrDefault(p.getId(), new int[5]);
                            String imageUrl = (p.getImage() != null && !p.getImage().isEmpty()) ? p.getImage().get(0) : null;
                            perfList.add(new ProductPerformance(p.getName(), purchase, stars, imageUrl, 0f, 0));
                        }
                        List<ProductPerformance> best = new ArrayList<>(perfList);
                        best.sort((a, b) -> Integer.compare(b.purchaseCount, a.purchaseCount));
                        List<ProductPerformance> topBest = best.subList(0, Math.min(10, best.size()));
                        adapterBest.updateData(topBest);
                    });
                });
            });
        });
    }

    public static class ProductPerformance {
        public String name;
        public int purchaseCount;
        public int[] starCounts; // [1*, 2*, 3*, 4*, 5*]
        public String imageUrl;
        public float averageRating;
        public int reviewCount;
        public ProductPerformance(String name, int purchaseCount, int[] starCounts, String imageUrl, float averageRating, int reviewCount) {
            this.name = name;
            this.purchaseCount = purchaseCount;
            this.starCounts = starCounts;
            this.imageUrl = imageUrl;
            this.averageRating = averageRating;
            this.reviewCount = reviewCount;
        }
    }

    public static class ProductPerformanceAdapter extends RecyclerView.Adapter<ProductPerformanceAdapter.ViewHolder> {
        private List<ProductPerformance> data;
        private boolean showRating;
        public ProductPerformanceAdapter(List<ProductPerformance> data, boolean showRating) { this.data = data; this.showRating = showRating; }
        public void updateData(List<ProductPerformance> newData) {
            this.data = newData;
            notifyDataSetChanged();
        }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_performance, parent, false);
            return new ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ProductPerformance item = data.get(position);
            holder.productName.setText(item.name);
            holder.purchaseCount.setText("Đã mua: " + item.purchaseCount + " lần");
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(holder.productImage.getContext())
                        .load(item.imageUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(holder.productImage);
            } else {
                holder.productImage.setImageResource(R.drawable.ic_launcher_background);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(holder.itemView.getContext(), android.R.layout.simple_spinner_item, new String[]{"1★", "2★", "3★", "4★", "5★"});
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.starSpinner.setAdapter(adapter);
            holder.starSpinner.setSelection(4);
            holder.starCount.setText("Số lượt đánh giá: " + item.starCounts[4]);
            holder.starSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                    holder.starCount.setText("Số lượt đánh giá: " + item.starCounts[pos]);
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            if (showRating) {
                holder.avgRating.setVisibility(View.VISIBLE);
                holder.avgRating.setText(String.format("★ %.2f (%d đánh giá)", item.averageRating, item.reviewCount));
            } else {
                holder.avgRating.setVisibility(View.GONE);
            }
        }
        @Override
        public int getItemCount() { return data != null ? data.size() : 0; }
        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView productImage;
            TextView productName, purchaseCount, starCount, avgRating;
            Spinner starSpinner;
            ViewHolder(View itemView) {
                super(itemView);
                productImage = itemView.findViewById(R.id.productImage);
                productName = itemView.findViewById(R.id.productName);
                purchaseCount = itemView.findViewById(R.id.purchaseCount);
                starSpinner = itemView.findViewById(R.id.starSpinner);
                starCount = itemView.findViewById(R.id.starCount);
                avgRating = itemView.findViewById(R.id.avgRating);
            }
        }
    }
} 