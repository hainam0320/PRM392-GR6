// ProductAdapter.java
package com.example.myapplication.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.EditProductActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private Context context;
    private List<Product> productList;

    public ProductAdapter(Context context, List<Product> list) {
        this.context = context;
        this.productList = list;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.textName.setText(product.getName());
        holder.textPrice.setText("₫" + product.getPrice());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditProductActivity.class);
            intent.putExtra("productId", product.getId());
            intent.putExtra("name", product.getName());
            intent.putExtra("price", product.getPrice());
            intent.putExtra("stock", product.getStock());
            intent.putExtra("brand", product.getBrand());
            intent.putExtra("category", product.getCategory());
            intent.putExtra("description", product.getDescription());
            if (product.getImage() != null && !product.getImage().isEmpty()) {
                intent.putExtra("image", product.getImage().get(0));
            }
            context.startActivity(intent);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (product.getId() == null) {
                Toast.makeText(context, "Lỗi: ID null", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseFirestore.getInstance()
                    .collection("products")
                    .document(product.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Đã xoá", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textPrice;
        Button btnDelete;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textProductName);
            textPrice = itemView.findViewById(R.id.textProductPrice);
            btnDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}
