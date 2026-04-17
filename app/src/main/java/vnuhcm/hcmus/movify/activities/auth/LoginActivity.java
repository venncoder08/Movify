package vnuhcm.hcmus.movify.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.activities.BookingActivity;

import android.widget.Button;

public class LoginActivity extends AppCompatActivity {
    TextView tvRegister;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tvRegister = findViewById(R.id.tvRegister);
        btnLogin = findViewById(R.id.btnLogin);

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, BookingActivity.class);
            startActivity(intent);
        });
    }
}
