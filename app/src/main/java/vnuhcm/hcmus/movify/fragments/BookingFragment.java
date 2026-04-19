package vnuhcm.hcmus.movify.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.adapters.PromotionAdapter;
import vnuhcm.hcmus.movify.models.Promotion;

public class BookingFragment extends Fragment {

    private EditText edtPickup, edtDestination;

    private LinearLayout btnMotorbike, btnCar, btnCash, btnBankTransfer, btnSearchDriver;
    private FrameLayout iconMotorbike, iconCar, iconCash, iconBank;
    private ImageView ivMotorbike, ivCar, ivCash, ivBankTransfer;
    private TextView tvBikeTitle, tvBikeDesc, tvCarTitle, tvCarDesc, tvCash, tvBankTransfer;
    private RecyclerView rvPromotions;

    private String selectedPromo = "";
    private String selectedVehicle = "Xe máy";
    private String selectedPayment = "Tiền mặt";

    public BookingFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_booking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupVehicleButtons();
        setupPaymentButtons();
        setupPromotions();
        setupSearchDriverButton();
    }

    private void initViews(View view) {
        edtPickup = view.findViewById(R.id.edtPickup);
        edtDestination = view.findViewById(R.id.edtDestination);

        btnMotorbike = view.findViewById(R.id.btnMotorbike);
        btnCar = view.findViewById(R.id.btnCar);
        btnCash = view.findViewById(R.id.btnCash);
        btnBankTransfer = view.findViewById(R.id.btnBankTransfer);
        btnSearchDriver = view.findViewById(R.id.btnSearchDriver);

        iconMotorbike = view.findViewById(R.id.iconMotorbike);
        iconCar = view.findViewById(R.id.iconCar);
        iconCash = view.findViewById(R.id.iconCash);
        iconBank = view.findViewById(R.id.iconBank);

        ivMotorbike = view.findViewById(R.id.ivMotorbike);
        ivCar = view.findViewById(R.id.ivCar);
        ivCash = view.findViewById(R.id.ivCash);
        ivBankTransfer = view.findViewById(R.id.ivBankTransfer);

        tvBikeTitle = view.findViewById(R.id.tvBikeTitle);
        tvBikeDesc = view.findViewById(R.id.tvBikeDesc);
        tvCarTitle = view.findViewById(R.id.tvCarTitle);
        tvCarDesc = view.findViewById(R.id.tvCarDesc);
        tvCash = view.findViewById(R.id.tvCash);
        tvBankTransfer = view.findViewById(R.id.tvBankTransfer);

        rvPromotions = view.findViewById(R.id.rvPromotions);
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

            ivMotorbike.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white));
            ivCar.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));

            tvBikeTitle.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            tvBikeDesc.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            tvCarTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvCarDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else {
            btnMotorbike.setBackgroundResource(R.drawable.bg_unselected);
            btnCar.setBackgroundResource(R.drawable.bg_selected);

            iconMotorbike.setBackgroundResource(R.drawable.bg_input);
            iconCar.setBackgroundResource(R.drawable.bg_icon_blue_soft);

            ivMotorbike.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
            ivCar.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white));

            tvBikeTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvBikeDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvCarTitle.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            tvCarDesc.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
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

            ivCash.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white));
            ivBankTransfer.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));

            tvCash.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            tvBankTransfer.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else {
            btnCash.setBackgroundResource(R.drawable.bg_unselected);
            btnBankTransfer.setBackgroundResource(R.drawable.bg_selected);

            iconCash.setBackgroundResource(R.drawable.bg_input);
            iconBank.setBackgroundResource(R.drawable.bg_icon_blue_soft);

            ivCash.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
            ivBankTransfer.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white));

            tvCash.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvBankTransfer.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        }
    }

    private void setupPromotions() {
        rvPromotions.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<Promotion> promotionList = new ArrayList<>();
        promotionList.add(new Promotion("GIAM10", "Giảm 10% cho chuyến đi"));
        promotionList.add(new Promotion("FREESHIP", "Giảm 15.000đ"));
        promotionList.add(new Promotion("NEWUSER", "Giảm 20%"));

        PromotionAdapter adapter = new PromotionAdapter(promotionList, promotion -> {
            selectedPromo = promotion.getTitle();
            Toast.makeText(requireContext(), "Đã chọn mã: " + selectedPromo, Toast.LENGTH_SHORT).show();
        });

        rvPromotions.setAdapter(adapter);
    }

    private void setupSearchDriverButton() {
        btnSearchDriver.setOnClickListener(v -> {
            String pickup = edtPickup.getText().toString().trim();
            String destination = edtDestination.getText().toString().trim();

            if (TextUtils.isEmpty(pickup)) {
                edtPickup.setError("Nhập điểm đón");
                return;
            }

            if (TextUtils.isEmpty(destination)) {
                edtDestination.setError("Nhập điểm đến");
                return;
            }

            Toast.makeText(
                    requireContext(),
                    "Đang tìm tài xế...\n"
                            + "Xe: " + selectedVehicle
                            + "\nThanh toán: " + selectedPayment
                            + (selectedPromo.isEmpty() ? "" : "\nKM: " + selectedPromo),
                    Toast.LENGTH_LONG
            ).show();
        });
    }
}