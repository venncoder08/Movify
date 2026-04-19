package vnuhcm.hcmus.movify.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.adapters.NotificationAdapter;
import vnuhcm.hcmus.movify.models.Notification;

public class NotificationFragment extends Fragment {

    private ListView lvNotifications;
    private TextView tvNewBadge;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;

    public NotificationFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        lvNotifications = view.findViewById(R.id.lvNotifications);
        tvNewBadge = view.findViewById(R.id.tvNewBadge);

        notificationList = new ArrayList<>();
        loadMockData();

        adapter = new NotificationAdapter(requireContext(), notificationList);
        lvNotifications.setAdapter(adapter);

        updateUnreadCount();

        lvNotifications.setOnItemClickListener((parent, v, position, id) -> {
            Notification item = notificationList.get(position);

            if (item.isUnread()) {
                item.setUnread(false);

                adapter.notifyDataSetChanged();
                updateUnreadCount();
            }
        });

        return view;
    }

    private void updateUnreadCount() {
        int count = 0;
        for (Notification item : notificationList) {
            if (item.isUnread()) {
                count++;
            }
        }
        tvNewBadge.setText(count + " mới");
    }

    private void loadMockData() {
        notificationList.add(new Notification(
                R.drawable.ic_search,
                "Đang tìm tài xế gần bạn",
                "Vui lòng chờ trong giây lát...",
                "1 phút trước",
                "Tất cả",
                true,
                false
        ));

        notificationList.add(new Notification(
                R.drawable.ic_success,
                "Tài xế đã nhận cuốc",
                "Nguyễn Văn A - 29A-12345 - ⭐ 4.8",
                "3 phút trước",
                "Chuyến đi",
                true,
                false
        ));

        notificationList.add(new Notification(
                R.drawable.ic_location,
                "Tài xế đang đến điểm đón",
                "Còn khoảng 5 phút nữa sẽ đến",
                "5 phút trước",
                "Chuyến đi",
                true,
                false
        ));

        notificationList.add(new Notification(
                R.drawable.ic_card,
                "Thanh toán thành công",
                "Số tiền: 45.000đ - Chuyến đi #12345",
                "15 phút trước",
                "Thanh toán",
                false,
                false
        ));

        notificationList.add(new Notification(
                R.drawable.ic_flag,
                "Chuyến đi đã hoàn thành",
                "Cám ơn bạn đã sử dụng dịch vụ",
                "20 phút trước",
                "Chuyến đi",
                false,
                false
        ));

        notificationList.add(new Notification(
                R.drawable.ic_star,
                "Mời bạn đánh giá tài xế",
                "Chia sẻ trải nghiệm của bạn",
                "25 phút trước",
                "Tất cả",
                false,
                false
        ));

        notificationList.add(new Notification(
                R.drawable.ic_close,
                "Tài xế đã hủy chuyến",
                "Vui lòng đặt lại chuyến đi",
                "1 giờ trước",
                "Hủy chuyến",
                false,
                true
        ));

        notificationList.add(new Notification(
                R.drawable.ic_refresh,
                "Tài xế thay đổi lộ trình",
                "Do kẹt xe trên đường Nguyễn Huệ",
                "3 ngày trước",
                "Chuyến đi",
                false,
                false
        ));
    }
}