package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.model.Order;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;

public class ShippingInfoActivity extends AppCompatActivity {
    private TextInputEditText nameInput;
    private TextInputEditText phoneInput;
    private TextInputEditText addressInput;
    private RadioGroup paymentMethodGroup;
    private TextView itemCountText;
    private TextView totalAmountText;
    private Button confirmButton;
    
    private FirebaseFirestore db;
    private String userId;
    private ArrayList<String> productIds;
    private ArrayList<Integer> quantities;
    private double totalAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipping_info);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Thông tin giao hàng");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get data from intent
        productIds = getIntent().getStringArrayListExtra("product_ids");
        quantities = getIntent().getIntegerArrayListExtra("quantities");
        totalAmount = getIntent().getDoubleExtra("total_amount", 0);

        // Initialize views
        initializeViews();
        
        // Update order summary
        updateOrderSummary();

        // Setup confirm button
        confirmButton.setOnClickListener(v -> validateAndPlaceOrder());
    }

    private void initializeViews() {
        nameInput = findViewById(R.id.nameInput);
        phoneInput = findViewById(R.id.phoneInput);
        addressInput = findViewById(R.id.addressInput);
        paymentMethodGroup = findViewById(R.id.paymentMethodGroup);
        itemCountText = findViewById(R.id.itemCountText);
        totalAmountText = findViewById(R.id.totalAmountText);
        confirmButton = findViewById(R.id.confirmButton);
    }

    private void updateOrderSummary() {
        int totalItems = 0;
        for (Integer quantity : quantities) {
            totalItems += quantity;
        }
        itemCountText.setText("Số lượng sản phẩm: " + totalItems);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        totalAmountText.setText("Tổng tiền: " + formatter.format(totalAmount));
    }

    private void validateAndPlaceOrder() {
        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Vui lòng nhập họ tên");
            nameInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Vui lòng nhập số điện thoại");
            phoneInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(address)) {
            addressInput.setError("Vui lòng nhập địa chỉ giao hàng");
            addressInput.requestFocus();
            return;
        }

        // Get selected payment method
        boolean isBankTransfer = paymentMethodGroup.getCheckedRadioButtonId() == R.id.bankPayment;
        String paymentMethod = isBankTransfer ? "BANK_TRANSFER" : "COD";

        // Show confirmation dialog
        showConfirmationDialog(name, phone, address, paymentMethod);
    }

    private void showConfirmationDialog(String name, String phone, String address, String paymentMethod) {
        String paymentText = paymentMethod.equals("BANK_TRANSFER") ? "Chuyển khoản ngân hàng" : "Thanh toán khi nhận hàng";
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        StringBuilder message = new StringBuilder();
        message.append("Thông tin đơn hàng:\n\n")
               .append("Họ tên: ").append(name).append("\n")
               .append("Số điện thoại: ").append(phone).append("\n")
               .append("Địa chỉ: ").append(address).append("\n")
               .append("Phương thức thanh toán: ").append(paymentText).append("\n")
               .append("Tổng tiền: ").append(formatter.format(totalAmount));

        if (paymentMethod.equals("BANK_TRANSFER")) {
            message.append("\n\nThông tin chuyển khoản:")
                   .append("\nNgân hàng: BIDV")
                   .append("\nSố tài khoản: 31410001234567")
                   .append("\nChủ tài khoản: CÔNG TY TNHH ABC")
                   .append("\nNội dung chuyển khoản: ").append(name).append(" - ").append(phone)
                   .append("\n\nLưu ý: Vui lòng chuyển khoản trước khi xác nhận đơn hàng.");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Xác nhận đơn hàng")
                .setMessage(message.toString())
                .setNegativeButton("Hủy", null);

        if (paymentMethod.equals("BANK_TRANSFER")) {
            builder.setPositiveButton("Đã chuyển khoản", (dialog, which) -> {
                showBankTransferConfirmation(name, phone, address, paymentMethod);
            });
        } else {
            builder.setPositiveButton("Xác nhận", (dialog, which) -> {
                placeOrder(name, phone, address, paymentMethod);
            });
        }

        builder.show();
    }

    private void showBankTransferConfirmation(String name, String phone, String address, String paymentMethod) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận chuyển khoản")
                .setMessage("Bạn đã hoàn tất chuyển khoản?\n\nLưu ý: Đơn hàng sẽ bị hủy nếu không nhận được tiền.")
                .setPositiveButton("Đã chuyển khoản", (dialog, which) -> {
                    placeOrder(name, phone, address, paymentMethod);
                })
                .setNegativeButton("Chưa chuyển khoản", null)
                .show();
    }

    private void placeOrder(String name, String phone, String address, String paymentMethod) {
        // Tạo đơn hàng mới
        Order order = new Order();
        order.setUserId(userId);
        order.setCustomerName(name);
        order.setCustomerPhone(phone);
        order.setShippingAddress(address);
        order.setTotalAmount(totalAmount);
        order.setPaymentMethod(paymentMethod);
        order.setStatus(paymentMethod.equals("BANK_TRANSFER") ? "Waiting_Payment" : "Pending");

        // Lưu thông tin đơn hàng
        db.collection("orders")
                .add(order)
                .addOnSuccessListener(documentReference -> {
                    String orderId = documentReference.getId();
                    // Set ID cho đơn hàng
                    order.setId(orderId);
                    // Cập nhật lại đơn hàng với ID
                    db.collection("orders")
                            .document(orderId)
                            .set(order)
                            .addOnSuccessListener(aVoid -> {
                                // Lưu chi tiết đơn hàng với thông tin sản phẩm đầy đủ
                                for (int i = 0; i < productIds.size(); i++) {
                                    String productId = productIds.get(i);
                                    int quantity = quantities.get(i);
                                    
                                    // Lấy thông tin sản phẩm từ Firestore
                                    db.collection("products").document(productId)
                                        .get()
                                        .addOnSuccessListener(productDoc -> {
                                            if (productDoc.exists()) {
                                                Map<String, Object> orderDetail = new HashMap<>();
                                                Map<String, Object> productData = new HashMap<>();
                                                
                                                // Copy toàn bộ thông tin sản phẩm
                                                productData.put("id", productDoc.getId());
                                                productData.put("name", productDoc.getString("name"));
                                                productData.put("price", productDoc.getDouble("price"));
                                                productData.put("brand", productDoc.getString("brand"));
                                                productData.put("category", productDoc.getString("category"));
                                                
                                                // Lưu hình ảnh dưới dạng List<String>
                                                @SuppressWarnings("unchecked")
                                                List<String> images = (List<String>) productDoc.get("image");
                                                if (images != null && !images.isEmpty()) {
                                                    productData.put("image", images);
                                                }
                                                
                                                orderDetail.put("product", productData);
                                                orderDetail.put("quantity", quantity);
                                                
                                                // Lưu chi tiết vào subcollection của đơn hàng
                                                db.collection("orders")
                                                    .document(orderId)
                                                    .collection("details")
                                                    .add(orderDetail)
                                                    .addOnFailureListener(e -> 
                                                        Toast.makeText(ShippingInfoActivity.this, 
                                                            "Lỗi khi lưu chi tiết đơn hàng", Toast.LENGTH_SHORT).show());
                                            }
                                        })
                                        .addOnFailureListener(e -> 
                                            Toast.makeText(ShippingInfoActivity.this, 
                                                "Lỗi khi lấy thông tin sản phẩm", Toast.LENGTH_SHORT).show());
                                }
                                
                                // Xóa giỏ hàng sau khi lưu xong
                                clearCartAndFinish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi cập nhật đơn hàng", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi đặt hàng", Toast.LENGTH_SHORT).show());
    }

    private void clearCartAndFinish() {
        // Xóa giỏ hàng
        db.collection("users")
                .document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Xóa từng sản phẩm trong giỏ hàng
                    queryDocumentSnapshots.forEach(document -> 
                        document.getReference().delete()
                    );
                    
                    // Thông báo thành công và kết thúc
                    handleSuccessfulPayment();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi xóa giỏ hàng", Toast.LENGTH_SHORT).show();
                    handleSuccessfulPayment(); // Vẫn kết thúc dù có lỗi
                });
    }

    private void handleSuccessfulPayment() {
        String message = paymentMethodGroup.getCheckedRadioButtonId() == R.id.bankPayment
            ? "Cảm ơn bạn đã đặt hàng! Đơn hàng sẽ được xử lý sau khi xác nhận thanh toán."
            : "Cảm ơn bạn đã mua hàng! Chúng tôi sẽ sớm giao hàng đến bạn.";

        new AlertDialog.Builder(this)
                .setTitle("Đặt hàng thành công")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 