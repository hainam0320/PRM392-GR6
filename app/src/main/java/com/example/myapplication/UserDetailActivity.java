package com.example.myapplication;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.model.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserDetailActivity extends AppCompatActivity {
    private TextView textUserId;
    private EditText editUsername, editEmail, editPhone, editAddress;
    private Spinner spinnerRole;
    private Button btnSave, btnDelete;
    private FirebaseFirestore db;
    private String userId;
    private User user;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        textUserId = findViewById(R.id.textUserId);
        editUsername = findViewById(R.id.editUsername);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        editAddress = findViewById(R.id.editAddress);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");
        if (userId != null) {
            loadUserDetail(userId);
        }

        btnSave.setOnClickListener(v -> saveRole());
        btnDelete.setOnClickListener(v -> confirmDeleteUser());
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadUserDetail(String userId) {
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            user = documentSnapshot.toObject(User.class);
            if (user != null) {
                user.setId(documentSnapshot.getId());
                displayUserInfo(user);
            }
        });
    }

    private void displayUserInfo(User user) {
        textUserId.setText("ID: " + user.getId());
        editUsername.setText(user.getUsername());
        editEmail.setText(user.getEmail());
        editPhone.setText(user.getPhone());
        editAddress.setText(user.getAddress());
        String[] roles = {"user", "admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equalsIgnoreCase(user.getRole())) {
                spinnerRole.setSelection(i);
                break;
            }
        }
    }

    private void saveRole() {
        String newRole = spinnerRole.getSelectedItem().toString();
        if (user != null && !newRole.equals(user.getRole())) {
            DocumentReference userRef = db.collection("users").document(userId);
            userRef.update("role", newRole).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Cập nhật role thành công", Toast.LENGTH_SHORT).show();
                user.setRole(newRole);
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi cập nhật role", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void confirmDeleteUser() {
        new AlertDialog.Builder(this)
            .setTitle("Xóa người dùng")
            .setMessage("Bạn có chắc chắn muốn xóa người dùng này không?")
            .setPositiveButton("Xóa", (dialog, which) -> deleteUser())
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void deleteUser() {
        db.collection("users").document(userId).delete().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Đã xóa người dùng", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi xóa người dùng", Toast.LENGTH_SHORT).show();
        });
    }
} 