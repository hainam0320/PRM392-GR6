package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.adapter.ProductAdapter;
import com.example.myapplication.model.Product;
import com.example.myapplication.model.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {
    private static final String TAG = "MainActivity";
    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private List<Product> allProducts;
    private ProgressBar progressBar;
    private TextView emptyView;
    private Spinner categorySpinner;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentSearchQuery = "";
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Shoe Store");
        }

        progressBar = findViewById(R.id.progressBar);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        categorySpinner = findViewById(R.id.categorySpinner);

        // Setup RecyclerView
        allProducts = new ArrayList<>();
        productsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(new ArrayList<>(), this);
        productsRecyclerView.setAdapter(productAdapter);

        // Load products
        loadProducts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                filterProducts(currentSearchQuery, selectedCategory);
                return true;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                currentSearchQuery = "";
                filterProducts("", selectedCategory);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            auth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_cart) {
            startActivity(new Intent(this, CartActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_edit_profile) {
            startActivity(new Intent(this, EditProfileActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_favorite) {
            startActivity(new Intent(this, FavoritesActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_orders) {
            startActivity(new Intent(this, OrderListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        productsRecyclerView.setVisibility(View.GONE);

        db.collection("products")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Product> products = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Product product = document.toObject(Product.class);
                                product.setId(document.getId());
                                products.add(product);
                                
                                // Calculate average rating for this product
                                String productId = document.getId();
                                db.collection("reviews")
                                    .whereEqualTo("productId", productId)
                                    .get()
                                    .addOnSuccessListener(reviewsSnapshot -> {
                                        if (!reviewsSnapshot.isEmpty()) {
                                            float totalRating = 0;
                                            int count = 0;
                                            for (QueryDocumentSnapshot reviewDoc : reviewsSnapshot) {
                                                float rating = reviewDoc.getDouble("rating").floatValue();
                                                totalRating += rating;
                                                count++;
                                            }
                                            float averageRating = totalRating / count;
                                            product.setAverageRating(averageRating);
                                            // Update the adapter to reflect the new rating
                                            productAdapter.notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(e -> 
                                        Log.e(TAG, "Error getting reviews for product " + productId, e));
                            } catch (Exception e) {
                                Log.e(TAG, "Error converting document " + document.getId(), e);
                            }
                        }
                        
                        progressBar.setVisibility(View.GONE);
                        productsRecyclerView.setVisibility(View.VISIBLE);
                        
                        allProducts.clear();
                        allProducts.addAll(products);
                        setupCategorySpinner();
                        productAdapter.updateProducts(products);
                        updateProductVisibility();
                        
                        if (products.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Không tìm thấy sản phẩm nào", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error getting products", task.getException());
                        Toast.makeText(MainActivity.this, "Lỗi khi tải sản phẩm", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupCategorySpinner() {
        List<String> categories = new ArrayList<>();
        categories.add("All");
        List<String> productCategories = allProducts.stream()
                .map(Product::getCategory)
                .distinct()
                .collect(Collectors.toList());
        categories.addAll(productCategories);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
                filterProducts(currentSearchQuery, selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateProductVisibility() {
        if (productAdapter.getItemCount() == 0) {
            productsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            productsRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void filterProducts(String query, String category) {
        List<Product> filteredList = new ArrayList<>();
        for (Product product : allProducts) {
            boolean matchesCategory = category.equals("All") || product.getCategory().equalsIgnoreCase(category);
            boolean matchesQuery = product.getName().toLowerCase().contains(query.toLowerCase());

            if (matchesCategory && matchesQuery) {
                filteredList.add(product);
            }
        }
        productAdapter.updateProducts(filteredList);
        updateProductVisibility();
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
    }

    @Override
    public void onAddToCartClick(Product product) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if product is out of stock
        if (product.getStock() <= 0) {
            Toast.makeText(this, "Sản phẩm " + product.getName() + " đã hết hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Check if product already exists in cart
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
                            Toast.makeText(MainActivity.this, 
                                "Không thể thêm vào giỏ hàng. Số lượng vượt quá hàng có sẵn.", 
                                Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Update existing cart item quantity
                        querySnapshot.getDocuments().get(0).getReference()
                            .update("quantity", newQuantity)
                            .addOnSuccessListener(aVoid -> 
                                Toast.makeText(MainActivity.this, 
                                    "Đã cập nhật số lượng trong giỏ hàng", 
                                    Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> 
                                Toast.makeText(MainActivity.this, 
                                    "Lỗi khi cập nhật giỏ hàng", 
                                    Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // Add new item to cart
                    CartItem cartItem = new CartItem(product, 1);
                    db.collection("users").document(userId)
                        .collection("cart")
                        .add(cartItem)
                        .addOnSuccessListener(documentReference -> 
                            Toast.makeText(MainActivity.this, 
                                "Đã thêm vào giỏ hàng: " + product.getName(), 
                                Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> 
                            Toast.makeText(MainActivity.this, 
                                "Lỗi khi thêm vào giỏ hàng", 
                                Toast.LENGTH_SHORT).show());
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(MainActivity.this, 
                    "Lỗi khi kiểm tra giỏ hàng", 
                    Toast.LENGTH_SHORT).show());
    }
}