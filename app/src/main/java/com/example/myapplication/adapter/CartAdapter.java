package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.CartItem;

import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private List<CartItem> cartItems;
    private CartItemClickListener listener;

    public interface CartItemClickListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onDeleteItem(CartItem item);
    }

    public CartAdapter(List<CartItem> cartItems, CartItemClickListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public void updateData(List<CartItem> newCartItems) {
        this.cartItems = newCartItems;
        notifyDataSetChanged();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        private ImageView productImage;
        private TextView productName;
        private TextView productPrice;
        private TextView quantityText;
        private ImageButton decreaseButton;
        private ImageButton increaseButton;
        private ImageButton deleteButton;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.cart_item_image);
            productName = itemView.findViewById(R.id.cart_item_name);
            productPrice = itemView.findViewById(R.id.cart_item_price);
            quantityText = itemView.findViewById(R.id.cart_item_quantity);
            decreaseButton = itemView.findViewById(R.id.decrease_quantity);
            increaseButton = itemView.findViewById(R.id.increase_quantity);
            deleteButton = itemView.findViewById(R.id.delete_item);
        }

        public void bind(CartItem item) {
            if (item.getProduct().getImage() != null && !item.getProduct().getImage().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getProduct().getImage().get(0))
                        .into(productImage);
            }

            productName.setText(item.getProduct().getName());
            
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            productPrice.setText(formatter.format(item.getProduct().getPrice()));
            
            quantityText.setText(String.valueOf(item.getQuantity()));

            decreaseButton.setOnClickListener(v -> {
                if (item.getQuantity() > 1) {
                    listener.onQuantityChanged(item, item.getQuantity() - 1);
                }
            });

            increaseButton.setOnClickListener(v -> {
                if (item.getQuantity() < item.getProduct().getStock()) {
                    listener.onQuantityChanged(item, item.getQuantity() + 1);
                }
            });

            deleteButton.setOnClickListener(v -> listener.onDeleteItem(item));
        }
    }
} 