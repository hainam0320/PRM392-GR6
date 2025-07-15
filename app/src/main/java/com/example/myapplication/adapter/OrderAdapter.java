package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.Order;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private List<Order> orders;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderAdapter(List<Order> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order, listener);
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    public void updateOrders(List<Order> newOrders) {
        this.orders.clear();
        this.orders.addAll(newOrders);
        notifyDataSetChanged();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private TextView textOrderId, textCustomerName, textStatus, textTotalAmount;

        OrderViewHolder(View itemView) {
            super(itemView);
            textOrderId = itemView.findViewById(R.id.textOrderId);
            textCustomerName = itemView.findViewById(R.id.textCustomerName);
            textStatus = itemView.findViewById(R.id.textStatus);
            textTotalAmount = itemView.findViewById(R.id.textTotalAmount);
        }

        void bind(final Order order, final OnOrderClickListener listener) {
            textOrderId.setText("Mã đơn: " + order.getId());
            textCustomerName.setText("Khách: " + order.getCustomerName());
            textStatus.setText("Trạng thái: " + order.getStatus());
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            textTotalAmount.setText("Tổng: " + format.format(order.getTotalAmount()));
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOrderClick(order);
            });
        }
    }
} 