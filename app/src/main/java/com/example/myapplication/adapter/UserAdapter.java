package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.User;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> users;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(List<User> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    public void updateUsers(List<User> newUsers) {
        this.users.clear();
        this.users.addAll(newUsers);
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView textUsername, textEmail, textRole;

        UserViewHolder(View itemView) {
            super(itemView);
            textUsername = itemView.findViewById(R.id.textUsername);
            textEmail = itemView.findViewById(R.id.textEmail);
            textRole = itemView.findViewById(R.id.textRole);
        }

        void bind(final User user, final OnUserClickListener listener) {
            textUsername.setText(user.getUsername());
            textEmail.setText(user.getEmail());
            textRole.setText("Role: " + user.getRole());
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onUserClick(user);
            });
        }
    }
} 