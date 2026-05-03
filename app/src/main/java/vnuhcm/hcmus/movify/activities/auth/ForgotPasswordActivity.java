package vnuhcm.hcmus.movify.activities.auth;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.utils.PasswordVisibilityToggle;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText edtEmail;
    private EditText edtOtp;
    private EditText edtNewPassword;
    private EditText edtConfirmPassword;
    private MaterialButton btnSendOtp;
    private MaterialButton btnResetPassword;
    private TextView tvBackToLogin;

    private AuthApiService authApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authApiService = new AuthApiService(this);

        initViews();
        bindActions();
        prefillEmail();
    }

    private void initViews() {
        edtEmail = findViewById(R.id.edtEmail);
        edtOtp = findViewById(R.id.edtOtp);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        PasswordVisibilityToggle.attach(edtNewPassword);
        PasswordVisibilityToggle.attach(edtConfirmPassword);
    }

    private void bindActions() {
        tvBackToLogin.setOnClickListener(v -> finish());
        btnSendOtp.setOnClickListener(v -> sendOtp());
        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void prefillEmail() {
        String prefillEmail = getIntent().getStringExtra("prefill_email");
        if (prefillEmail != null && !prefillEmail.trim().isEmpty()) {
            edtEmail.setText(prefillEmail.trim());
        }
    }

    private void sendOtp() {
        String email = edtEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendOtp.setEnabled(false);
        btnSendOtp.setText("Đang gửi");

        new Thread(() -> {
            try {
                String message = authApiService.requestOTP(email);
                runOnUiThread(() -> {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    resetOtpButton();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    resetOtpButton();
                });
            }
        }).start();
    }

    private void resetOtpButton() {
        btnSendOtp.setEnabled(true);
        btnSendOtp.setText(R.string.send);
    }

    private void resetPassword() {
        String email = edtEmail.getText().toString().trim();
        String otp = edtOtp.getText().toString().trim();
        String newPassword = edtNewPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_required_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Đang cập nhật...");

        new Thread(() -> {
            try {
                String message = authApiService.resetPasswordWithOTP(email, otp, newPassword);
                runOnUiThread(() -> {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    resetPasswordButton();
                });
            }
        }).start();
    }

    private void resetPasswordButton() {
        btnResetPassword.setEnabled(true);
        btnResetPassword.setText(getString(R.string.reset_password_button));
    }
}
