package vnuhcm.hcmus.movify.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.activities.HomeActivity;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.utils.PasswordVisibilityToggle;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtFullName;
    private EditText edtPhoneNumber;
    private EditText edtEmail;
    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private EditText edtVerifyCode;
    private MaterialButton btnSend;
    private MaterialButton btnRegister;
    private TextView tvLogin;

    private AuthApiService authApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authApiService = new AuthApiService(this);

        initViews();
        bindActions();
    }

    private void initViews() {
        edtFullName = findViewById(R.id.edtFullName);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        edtVerifyCode = findViewById(R.id.edtVerifyCode);
        btnSend = findViewById(R.id.btnSend);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        PasswordVisibilityToggle.attach(edtPassword);
        PasswordVisibilityToggle.attach(edtConfirmPassword);
    }

    private void bindActions() {
        tvLogin.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> requestRegisterOtp());
        btnRegister.setOnClickListener(v -> registerAccount());
    }

    private void requestRegisterOtp() {
        String email = edtEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email trước", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);
        btnSend.setText("Đang gửi");

        new Thread(() -> {
            try {
                String message = authApiService.requestOTP(email, "verify-email");
                runOnUiThread(() -> {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    resetSendOtpButton();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    resetSendOtpButton();
                });
            }
        }).start();
    }

    private void resetSendOtpButton() {
        btnSend.setEnabled(true);
        btnSend.setText(R.string.send);
    }

    private void registerAccount() {
        String fullName = edtFullName.getText().toString().trim();
        String phoneNumber = edtPhoneNumber.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();
        String otp = edtVerifyCode.getText().toString().trim();

        if (fullName.isEmpty() || phoneNumber.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty() || otp.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_required_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (phoneNumber.length() < 10) {
            Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Đang tạo tài khoản...");

        new Thread(() -> {
            try {
                String token = authApiService.registerWithOTP(fullName, phoneNumber, email, otp, password);
                runOnUiThread(() -> {
                    if (token == null || token.isEmpty()) {
                        Toast.makeText(this, "Đăng ký thất bại", Toast.LENGTH_SHORT).show();
                        resetRegisterButton();
                        return;
                    }

                    Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, HomeActivity.class);
                    startActivity(intent);
                    finishAffinity();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    resetRegisterButton();
                });
            }
        }).start();
    }

    private void resetRegisterButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText(R.string.register);
    }
}
