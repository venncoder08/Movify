package vnuhcm.hcmus.movify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.activities.auth.LoginActivity;
import vnuhcm.hcmus.movify.services.AuthApiService;

public class LoadingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        AuthApiService authApiService = new AuthApiService(this);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Class<?> nextActivity = authApiService.isLoggedIn() ? HomeActivity.class : LoginActivity.class;
                Intent intent = new Intent(LoadingActivity.this, nextActivity);
                startActivity(intent);
                finish();
            }
        }, 2000);
    }
}
