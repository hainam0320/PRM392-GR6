package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.CartItem;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OrderDetailAdapter extends RecyclerView.Adapter<OrderDetailAdapter.OrderDetailViewHolder> {
    private List<CartItem> items;
    private final NumberFormat currencyFormat;

    public OrderDetailAdapter(List<CartItem> items) {
        this.items = items;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    @NonNull
    @Override
    public OrderDetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_detail, parent, false);
        return new OrderDetailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderDetailViewHolder holder, int position) {
        CartItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class OrderDetailViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productName;
        private final TextView productPrice;
        private final TextView productQuantity;
        private final TextView productTotal;

        OrderDetailViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productQuantity = itemView.findViewById(R.id.productQuantity);
            productTotal = itemView.findViewById(R.id.productTotal);
        }

        void bind(CartItem item) {
            try {
                if (item == null || item.getProduct() == null) {
                    productName.setText("Sản phẩm không xác định");
                    productPrice.setText("0 ₫");
                    productQuantity.setText("x0");
                    productTotal.setText("0 ₫");
                    productImage.setImageResource(R.drawable.ic_launcher_background);
                    return;
                }

                // Load product image
                List<String> images = item.getProduct().getImage();
                if (images != null && !images.isEmpty() && images.get(0) != null) {
                    Glide.with(itemView.getContext())
                            .load(images.get(0))
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .into(productImage);
                } else {
                    productImage.setImageResource(R.drawable.ic_launcher_background);
                }

                // Set product details
                productName.setText(item.getProduct().getName() != null ? 
                    item.getProduct().getName() : "Sản phẩm không xác định");
                
                double price = item.getProduct().getPrice();
                productPrice.setText(currencyFormat.format(price));
                productQuantity.setText("x" + item.getQuantity());
                productTotal.setText(currencyFormat.format(price * item.getQuantity()));
            } catch (Exception e) {
                // Log error but don't crash
                e.printStackTrace();
                // Set default values
                productName.setText("Lỗi hiển thị sản phẩm");
                productPrice.setText("0 ₫");
                productQuantity.setText("x0");
                productTotal.setText("0 ₫");
                productImage.setImageResource(R.drawable.ic_launcher_background);
            }
        }
    }
} 