package vnuhcm.hcmus.movify.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.adapters.HistoryAdapter;
import vnuhcm.hcmus.movify.models.History;

public class HistoryFragment extends Fragment {

    private ListView lvHistory;
    private HistoryAdapter adapter;
    private List<History> historyList;

    public HistoryFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        lvHistory = view.findViewById(R.id.lvHistory);

        historyList = new ArrayList<>();
        loadMockData();

        adapter = new HistoryAdapter(requireContext(), historyList);
        lvHistory.setAdapter(adapter);

        return view;
    }

    private void loadMockData() {
        historyList.add(new History(
                12345,
                "Hoàn thành",
                "15/03/2024 • 14:30",
                "Số 123 Nguyễn Huệ, Q.1",
                "Số 456 Lê Lợi, Q.3",
                "Nguyễn Văn A",
                "29A-12345",
                "15 phút",
                "5.2 km",
                "45.000đ",
                R.drawable.ic_motorbike
        ));

        historyList.add(new History(
                12344,
                "Hoàn thành",
                "14/03/2024 • 09:15",
                "Số 789 Trần Hưng Đạo, Q.5",
                "Số 321 Võ Văn Tần, Q.3",
                "Trần Văn B",
                "30B-67890",
                "18 phút",
                "6.8 km",
                "52.000đ",
                R.drawable.ic_motorbike
        ));

        historyList.add(new History(
                12343,
                "Đã hủy",
                "13/03/2024 • 16:45",
                "Số 555 Hai Bà Trưng, Q.1",
                "Số 888 Điện Biên Phủ, Q.3",
                "Lê Văn C",
                "29C-11111",
                "12 phút",
                "4.1 km",
                "38.000đ",
                R.drawable.ic_car
        ));

        historyList.add(new History(
                12342,
                "Hoàn thành",
                "12/03/2024 • 20:10",
                "Số 12 Phan Xích Long, Phú Nhuận",
                "Số 90 Cộng Hòa, Tân Bình",
                "Phạm Văn D",
                "51H-22222",
                "22 phút",
                "8.4 km",
                "67.000đ",
                R.drawable.ic_car
        ));
    }
}