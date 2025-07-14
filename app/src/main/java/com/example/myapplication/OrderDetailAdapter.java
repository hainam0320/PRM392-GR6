package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.model.CartItem;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OrderDetailAdapter extends RecyclerView.Adapter<OrderDetailAdapter.DetailViewHolder> {
    private List<CartItem> items;

    public OrderDetailAdapter(List<CartItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public DetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_detail, parent, false);
        return new DetailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DetailViewHolder holder, int position) {
        CartItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class DetailViewHolder extends RecyclerView.ViewHolder {
        private TextView textProductName, textQuantity, textPrice;

        DetailViewHolder(View itemView) {
            super(itemView);
            textProductName = itemView.findViewById(R.id.textProductName);
            textQuantity = itemView.findViewById(R.id.textQuantity);
            textPrice = itemView.findViewById(R.id.textPrice);
        }

        void bind(CartItem item) {
            textProductName.setText(item.getProduct() != null ? item.getProduct().getName() : "");
            textQuantity.setText("Số lượng: " + item.getQuantity());
            double price = (item.getProduct() != null) ? item.getProduct().getPrice() : 0;
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            textPrice.setText("Giá: " + format.format(price));
        }
    }
} 