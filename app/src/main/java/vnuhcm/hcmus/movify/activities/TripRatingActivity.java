package vnuhcm.hcmus.movify.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.services.RatingService;

public class TripRatingActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP_ID = "vnuhcm.hcmus.movify.extra.TRIP_ID";
    public static final String EXTRA_DRIVER_NAME = "vnuhcm.hcmus.movify.extra.DRIVER_NAME";

    private ExecutorService executorService;
    private String tripId;
    private RatingBar ratingBar;
    private EditText edtRatingComment;
    private Button btnSubmitRating;
    private Button btnSkipRating;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_rating);

        executorService = Executors.newSingleThreadExecutor();
        tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        if (TextUtils.isEmpty(tripId)) {
            finish();
            return;
        }

        TextView tvDriverName = findViewById(R.id.tvRatingDriverName);
        ratingBar = findViewById(R.id.ratingBarDriver);
        edtRatingComment = findViewById(R.id.edtRatingComment);
        btnSubmitRating = findViewById(R.id.btnSubmitRating);
        btnSkipRating = findViewById(R.id.btnSkipRating);

        String driverName = getIntent().getStringExtra(EXTRA_DRIVER_NAME);
        tvDriverName.setText(TextUtils.isEmpty(driverName)
                ? getString(R.string.rating_driver_fallback)
                : driverName);

        btnSubmitRating.setOnClickListener(v -> submitRating());
        btnSkipRating.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    private void submitRating() {
        float score = ratingBar.getRating();
        if (score <= 0f) {
            Toast.makeText(this, R.string.rating_score_required, Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitRating.setEnabled(false);
        btnSkipRating.setEnabled(false);
        btnSubmitRating.setText(R.string.rating_submitting);

        String comment = edtRatingComment.getText().toString().trim();
        executorService.execute(() -> {
            try {
                String message = RatingService.rateDriver(
                        getApplicationContext(),
                        tripId,
                        score,
                        comment
                );
                runOnUiThread(() -> {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    btnSubmitRating.setEnabled(true);
                    btnSkipRating.setEnabled(true);
                    btnSubmitRating.setText(R.string.rating_submit);
                    Toast.makeText(
                            this,
                            getString(R.string.rating_submit_failed, exception.getMessage()),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }
}
