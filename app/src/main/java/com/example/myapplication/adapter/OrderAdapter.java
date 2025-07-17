package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.Order;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private List<Order> orders;
    private final OnOrderClickListener listener;
    private final SimpleDateFormat dateFormat;
    private final NumberFormat currencyFormat;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderAdapter(List<Order> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
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
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void updateOrders(List<Order> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textOrderId;
        private final TextView textOrderDate;
        private final TextView textTotalAmount;
        private final TextView textStatus;
        private final TextView textPaymentMethod;

        OrderViewHolder(View itemView) {
            super(itemView);
            textOrderId = itemView.findViewById(R.id.textOrderId);
            textOrderDate = itemView.findViewById(R.id.textOrderDate);
            textTotalAmount = itemView.findViewById(R.id.textTotalAmount);
            textStatus = itemView.findViewById(R.id.textStatus);
            textPaymentMethod = itemView.findViewById(R.id.textPaymentMethod);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onOrderClick(orders.get(position));
                }
            });
        }

        void bind(Order order) {
            textOrderId.setText("Đơn hàng #" + order.getId().substring(0, 8));
            textOrderDate.setText(dateFormat.format(new Date(order.getOrderDate())));
            textTotalAmount.setText(currencyFormat.format(order.getTotalAmount()));
            
            // Set payment method
            String paymentText = order.getPaymentMethod().equals("BANK_TRANSFER") ? 
                               "Chuyển khoản" : "COD";
            textPaymentMethod.setText(paymentText);

            // Set status with color
            String status = order.getStatus();
            int statusColor;
            String statusText;

            switch (status.toLowerCase()) {
                case "pending":
                    statusText = "Chờ xử lý";
                    statusColor = R.color.status_pending;
                    break;
                case "waiting_payment":
                    statusText = "Chờ thanh toán";
                    statusColor = R.color.status_waiting;
                    break;
                case "confirmed":
                    statusText = "Đã xác nhận";
                    statusColor = R.color.status_confirmed;
                    break;
                case "shipping":
                    statusText = "Đang giao hàng";
                    statusColor = R.color.status_shipping;
                    break;
                case "completed":
                    statusText = "Hoàn thành";
                    statusColor = R.color.status_completed;
                    break;
                case "cancelled":
                    statusText = "Đã hủy";
                    statusColor = R.color.status_cancelled;
                    break;
                default:
                    statusText = status;
                    statusColor = R.color.status_pending;
            }

            textStatus.setText(statusText);
            textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), statusColor));
        }
    }
} 