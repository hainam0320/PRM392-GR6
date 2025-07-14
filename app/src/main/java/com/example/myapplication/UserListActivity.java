package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.UserAdapter;
import com.example.myapplication.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {
    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private List<User> userList;
    private List<User> allUsers;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        allUsers = new ArrayList<>();
        userAdapter = new UserAdapter(userList, this);
        recyclerViewUsers.setAdapter(userAdapter);
        db = FirebaseFirestore.getInstance();
        loadUsers();
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").addSnapshotListener((value, error) -> {
            progressBar.setVisibility(View.GONE);
            if (error != null || value == null) {
                Toast.makeText(this, "Lỗi tải người dùng: " + error, Toast.LENGTH_SHORT).show();
                return;
            }
            allUsers.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                try {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        user.setId(doc.getId());
                        allUsers.add(user);
                    }
                } catch (Exception e) {
                    // Bỏ qua user lỗi
                }
            }
            userAdapter.updateUsers(allUsers);
            updateUserVisibility();
            userList.clear();
            userList.addAll(allUsers);
        });
    }

    private void updateUserVisibility() {
        if (userAdapter.getItemCount() == 0) {
            recyclerViewUsers.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerViewUsers.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUserClick(User user) {
        Intent intent = new Intent(this, UserDetailActivity.class);
        intent.putExtra("userId", user.getId());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 