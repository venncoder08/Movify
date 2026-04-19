package vnuhcm.hcmus.movify.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import vnuhcm.hcmus.movify.R;

public class ProfileFragment extends Fragment {

    private TextView tvUserName, tvPhone, tvEmail, btnEditProfile;

    private View itemPersonalInfo, itemPaymentMethod, itemSavedAddress;
    private View itemNotification, itemLocation;
    private View itemHelpCenter, itemPolicy;

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupProfileHeader();
        setupAccountInfo();
        setupSettings();
        setupSupport();
    }

    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvEmail = view.findViewById(R.id.tvEmail);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        itemPersonalInfo = view.findViewById(R.id.itemPersonalInfo);
        itemPaymentMethod = view.findViewById(R.id.itemPaymentMethod);
        itemSavedAddress = view.findViewById(R.id.itemSavedAddress);

        itemNotification = view.findViewById(R.id.itemNotification);
        itemLocation = view.findViewById(R.id.itemLocation);

        itemHelpCenter = view.findViewById(R.id.itemHelpCenter);
        itemPolicy = view.findViewById(R.id.itemPolicy);
    }

    private void setupProfileHeader() {
        tvUserName.setText("Nguyễn Văn A");
        tvPhone.setText("+84 123 456 789");
        tvEmail.setText("nguyenvana@email.com");

        btnEditProfile.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mở chỉnh sửa hồ sơ", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupAccountInfo() {
        setupArrowItem(
                itemPersonalInfo,
                R.drawable.ic_user,
                "Thông tin cá nhân",
                "Họ tên, ngày sinh, giới tính"
        );

        setupArrowItem(
                itemPaymentMethod,
                R.drawable.ic_wallet,
                "Phương thức thanh toán",
                "Thẻ, ví điện tử"
        );

        setupArrowItem(
                itemSavedAddress,
                R.drawable.ic_location,
                "Địa chỉ đã lưu",
                "Nhà, công ty, địa điểm yêu thích"
        );

        itemPersonalInfo.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mở Thông tin cá nhân", Toast.LENGTH_SHORT).show()
        );

        itemPaymentMethod.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mở Phương thức thanh toán", Toast.LENGTH_SHORT).show()
        );

        itemSavedAddress.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mở Địa chỉ đã lưu", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupSettings() {
        setupSwitchItem(
                itemNotification,
                R.drawable.ic_noti,
                "Thông báo",
                "Quản lý thông báo ứng dụng",
                true
        );

        setupSwitchItem(
                itemLocation,
                R.drawable.ic_location,
                "Vị trí",
                "Cho phép truy cập vị trí",
                true
        );
    }

    private void setupSupport() {
        setupArrowItem(
                itemHelpCenter,
                R.drawable.ic_help,
                "Trung tâm trợ giúp",
                "Câu hỏi thường gặp, hướng dẫn"
        );

        setupArrowItem(
                itemPolicy,
                R.drawable.ic_policy,
                "Điều khoản & Chính sách",
                "Quy định sử dụng dịch vụ"
        );

        itemHelpCenter.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mở Trung tâm trợ giúp", Toast.LENGTH_SHORT).show()
        );

        itemPolicy.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mở Điều khoản & Chính sách", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupArrowItem(View itemView, int iconRes, String title, String subtitle) {
        ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
        TextView tvTitle = itemView.findViewById(R.id.tvTitle);
        TextView tvSubtitle = itemView.findViewById(R.id.tvSubtitle);

        ivIcon.setImageResource(iconRes);
        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
    }

    private void setupSwitchItem(View itemView, int iconRes, String title, String subtitle, boolean checked) {
        ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
        TextView tvTitle = itemView.findViewById(R.id.tvTitle);
        TextView tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        SwitchCompat switchItem = itemView.findViewById(R.id.switchItem);

        ivIcon.setImageResource(iconRes);
        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
        switchItem.setChecked(checked);

        switchItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = title + ": " + (isChecked ? "Bật" : "Tắt");
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }
}
