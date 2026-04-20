package vnuhcm.hcmus.movify.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.activities.HomeActivity;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.utils.PasswordVisibilityToggle;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    private TextView tvRegister;
    private TextView tvForgotPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnGoogleSignIn;
    private EditText edtEmail;
    private EditText edtPassword;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private AuthApiService authApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        authApiService = new AuthApiService(this);

        initViews();
        configureGoogleSignIn();
        bindActions();
    }

    private void initViews() {
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);

        PasswordVisibilityToggle.attach(edtPassword);
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
    }

    private void bindActions() {
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            intent.putExtra("prefill_email", edtEmail.getText().toString().trim());
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> loginWithEmailPassword());
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
    }

    private void loginWithEmailPassword() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText(R.string.logging_in);

        new Thread(() -> {
            try {
                String jwtToken = authApiService.loginWithEmail(email, password);
                if (jwtToken != null && !jwtToken.isEmpty()) {
                    runOnUiThread(this::navigateToHome);
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                        resetLoginButton();
                    });
                }
            } catch (Exception exception) {
                Log.e(TAG, "Email login error", exception);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.error_prefix) + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    resetLoginButton();
                });
            }
        }).start();
    }

    private void resetLoginButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText(R.string.login);
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != RC_SIGN_IN) {
            return;
        }

        Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            com.google.android.gms.auth.api.signin.GoogleSignInAccount account =
                    task.getResult(ApiException.class);
            if (account != null && account.getIdToken() != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException exception) {
            Log.w(TAG, "Google sign in failed", exception);
            Toast.makeText(this, R.string.google_sign_in_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        btnGoogleSignIn.setEnabled(false);
        btnGoogleSignIn.setText(R.string.authenticating);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        exchangeTokenWithBackend(idToken);
                    } else {
                        Log.w(TAG, "Firebase authentication failed", task.getException());
                        Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_SHORT).show();
                        resetGoogleButton();
                    }
                });
    }

    private void exchangeTokenWithBackend(String idToken) {
        new Thread(() -> {
            try {
                String jwtToken = authApiService.authenticateWithGoogle(idToken);
                if (jwtToken != null && !jwtToken.isEmpty()) {
                    runOnUiThread(this::navigateToHome);
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.token_error, Toast.LENGTH_SHORT).show();
                        resetGoogleButton();
                    });
                }
            } catch (Exception exception) {
                Log.e(TAG, "Token exchange error", exception);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.error_prefix) + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    resetGoogleButton();
                });
            }
        }).start();
    }

    private void resetGoogleButton() {
        btnGoogleSignIn.setEnabled(true);
        btnGoogleSignIn.setText(R.string.sign_in_with_google);
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null && authApiService.isLoggedIn()) {
            navigateToHome();
        }
    }
}
