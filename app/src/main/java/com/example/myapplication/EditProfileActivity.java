package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private TextInputEditText editTextUsername, editTextEmail, editTextPhone, editTextAddress;
    private Button buttonSaveProfile;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextUsername = findViewById(R.id.editTextUsername_edit);
        editTextEmail = findViewById(R.id.editTextEmail_edit);
        editTextPhone = findViewById(R.id.editTextPhone_edit);
        editTextAddress = findViewById(R.id.editTextAddress_edit);
        buttonSaveProfile = findViewById(R.id.buttonSaveProfile);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String userId = user.getUid();
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String email = documentSnapshot.getString("email");
                String phone = documentSnapshot.getString("phone");
                String address = documentSnapshot.getString("address");
                editTextUsername.setText(username);
                editTextEmail.setText(email);
                editTextPhone.setText(phone);
                editTextAddress.setText(address);
            }
        });

        buttonSaveProfile.setOnClickListener(v -> {
            String newUsername = editTextUsername.getText().toString().trim();
            String newEmail = editTextEmail.getText().toString().trim();
            String newPhone = editTextPhone.getText().toString().trim();
            String newAddress = editTextAddress.getText().toString().trim();
            if (newUsername.isEmpty()) {
                editTextUsername.setError("Tên người dùng không được để trống");
                return;
            }
            if (newEmail.isEmpty()) {
                editTextEmail.setError("Email không được để trống");
                return;
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("username", newUsername);
            updates.put("email", newEmail);
            updates.put("phone", newPhone);
            updates.put("address", newAddress);
            userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 