package vnuhcm.hcmus.movify.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.adapters.PromotionAdapter;
import vnuhcm.hcmus.movify.models.Promotion;

public class BookingActivity extends AppCompatActivity {

    private EditText edtPickup, edtDestination;

    private LinearLayout btnMotorbike, btnCar, btnCash, btnBankTransfer, btnSearchDriver;
    private FrameLayout iconMotorbike, iconCar, iconCash, iconBank;
    private ImageView ivMotorbike, ivCar, ivCash, ivBankTransfer;
    private TextView tvBikeTitle, tvBikeDesc, tvCarTitle, tvCarDesc, tvCash, tvBankTransfer;
    private RecyclerView rvPromotions;
    private String selectedPromo = "";

    private String selectedVehicle = "Xe máy";
    private String selectedPayment = "Tiền mặt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        initViews();
        setupVehicleButtons();
        setupPaymentButtons();
        setupPromotions();
        setupSearchDriverButton();
    }

    private void initViews() {
        edtPickup = findViewById(R.id.edtPickup);
        edtDestination = findViewById(R.id.edtDestination);

        btnMotorbike = findViewById(R.id.btnMotorbike);
        btnCar = findViewById(R.id.btnCar);
        btnCash = findViewById(R.id.btnCash);
        btnBankTransfer = findViewById(R.id.btnBankTransfer);
        btnSearchDriver = findViewById(R.id.btnSearchDriver);

        iconMotorbike = findViewById(R.id.iconMotorbike);
        iconCar = findViewById(R.id.iconCar);
        iconCash = findViewById(R.id.iconCash);
        iconBank = findViewById(R.id.iconBank);

        ivMotorbike = findViewById(R.id.ivMotorbike);
        ivCar = findViewById(R.id.ivCar);
        ivCash = findViewById(R.id.ivCash);
        ivBankTransfer = findViewById(R.id.ivBankTransfer);

        tvBikeTitle = findViewById(R.id.tvBikeTitle);
        tvBikeDesc = findViewById(R.id.tvBikeDesc);
        tvCarTitle = findViewById(R.id.tvCarTitle);
        tvCarDesc = findViewById(R.id.tvCarDesc);
        tvCash = findViewById(R.id.tvCash);
        tvBankTransfer = findViewById(R.id.tvBankTransfer);

        rvPromotions = findViewById(R.id.rvPromotions);
    }

    private void setupVehicleButtons() {
        updateVehicleUI();

        btnMotorbike.setOnClickListener(v -> {
            selectedVehicle = "Xe máy";
            updateVehicleUI();
        });

        btnCar.setOnClickListener(v -> {
            selectedVehicle = "Ô tô";
            updateVehicleUI();
        });
    }

    private void updateVehicleUI() {
        if ("Xe máy".equals(selectedVehicle)) {
            btnMotorbike.setBackgroundResource(R.drawable.bg_selected);
            btnCar.setBackgroundResource(R.drawable.bg_unselected);

            iconMotorbike.setBackgroundResource(R.drawable.bg_icon_blue_soft);
            iconCar.setBackgroundResource(R.drawable.bg_input);

            ivMotorbike.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
            ivCar.setColorFilter(ContextCompat.getColor(this, R.color.primary));

            tvBikeTitle.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvBikeDesc.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvCarTitle.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvCarDesc.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            btnMotorbike.setBackgroundResource(R.drawable.bg_unselected);
            btnCar.setBackgroundResource(R.drawable.bg_selected);

            iconMotorbike.setBackgroundResource(R.drawable.bg_input);
            iconCar.setBackgroundResource(R.drawable.bg_icon_blue_soft);

            ivMotorbike.setColorFilter(ContextCompat.getColor(this, R.color.primary));
            ivCar.setColorFilter(ContextCompat.getColor(this, android.R.color.white));

            tvBikeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvBikeDesc.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvCarTitle.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvCarDesc.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private void setupPaymentButtons() {
        updatePaymentUI();

        btnCash.setOnClickListener(v -> {
            selectedPayment = "Tiền mặt";
            updatePaymentUI();
        });

        btnBankTransfer.setOnClickListener(v -> {
            selectedPayment = "Chuyển khoản";
            updatePaymentUI();
        });
    }

    private void updatePaymentUI() {
        if ("Tiền mặt".equals(selectedPayment)) {
            btnCash.setBackgroundResource(R.drawable.bg_selected);
            btnBankTransfer.setBackgroundResource(R.drawable.bg_unselected);

            iconCash.setBackgroundResource(R.drawable.bg_icon_blue_soft);
            iconBank.setBackgroundResource(R.drawable.bg_input);

            ivCash.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
            ivBankTransfer.setColorFilter(ContextCompat.getColor(this, R.color.primary));

            tvCash.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvBankTransfer.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            btnCash.setBackgroundResource(R.drawable.bg_unselected);
            btnBankTransfer.setBackgroundResource(R.drawable.bg_selected);

            iconCash.setBackgroundResource(R.drawable.bg_input);
            iconBank.setBackgroundResource(R.drawable.bg_icon_blue_soft);

            ivCash.setColorFilter(ContextCompat.getColor(this, R.color.primary));
            ivBankTransfer.setColorFilter(ContextCompat.getColor(this, android.R.color.white));

            tvCash.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvBankTransfer.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private void setupPromotions() {
        rvPromotions = findViewById(R.id.rvPromotions);
        rvPromotions.setLayoutManager(new LinearLayoutManager(this));

        List<Promotion> promotionList = new ArrayList<>();
        promotionList.add(new Promotion("GIAM10", "Giảm 10% cho chuyến đi"));
        promotionList.add(new Promotion("FREESHIP", "Giảm 15.000đ cho chuyến đi"));
        promotionList.add(new Promotion("NEWUSER", "Giảm 20% cho người mới"));

        PromotionAdapter adapter = new PromotionAdapter(promotionList, promotion -> {
            selectedPromo = promotion.getTitle();
            Toast.makeText(this, "Đã chọn mã: " + selectedPromo, Toast.LENGTH_SHORT).show();
        });

        rvPromotions.setAdapter(adapter);
    }

    private void setupSearchDriverButton() {
        btnSearchDriver.setOnClickListener(v -> {
            String pickup = edtPickup.getText().toString().trim();
            String destination = edtDestination.getText().toString().trim();

            if (pickup.isEmpty()) {
                edtPickup.setError("Vui lòng nhập điểm đón");
                edtPickup.requestFocus();
                return;
            }

            if (destination.isEmpty()) {
                edtDestination.setError("Vui lòng nhập điểm đến");
                edtDestination.requestFocus();
                return;
            }

            Toast.makeText(
                    this,
                    "Đang tìm tài xế...\n"
                            + "Loại xe: " + selectedVehicle + "\n"
                            + "Thanh toán: " + selectedPayment
                            + (selectedPromo.isEmpty() ? "" : "\nKhuyến mãi: " + selectedPromo),
                    Toast.LENGTH_LONG
            ).show();
        });
    }
}