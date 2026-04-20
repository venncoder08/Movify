package vnuhcm.hcmus.movify.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.adapters.HistoryAdapter;
import vnuhcm.hcmus.movify.models.History;
import vnuhcm.hcmus.movify.models.HistoryScreenData;
import vnuhcm.hcmus.movify.services.HistoryService;

public class HistoryFragment extends Fragment {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ListView lvHistory;
    private TextView tvSummarySub;
    private TextView tvSummaryAmount;
    private TextView tvTotalTrips;
    private TextView tvTotalDistance;
    private TextView tvEmptyState;
    private HistoryAdapter adapter;
    private final List<History> historyList = new ArrayList<>();
    private ExecutorService historyExecutor;

    public HistoryFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        lvHistory = view.findViewById(R.id.lvHistory);
        tvSummarySub = view.findViewById(R.id.tvSummarySub);
        tvSummaryAmount = view.findViewById(R.id.tvSummaryAmount);
        tvTotalTrips = view.findViewById(R.id.tvTotalTrips);
        tvTotalDistance = view.findViewById(R.id.tvTotalDistance);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        historyExecutor = Executors.newSingleThreadExecutor();
        adapter = new HistoryAdapter(requireContext(), historyList);
        lvHistory.setAdapter(adapter);
        lvHistory.setEmptyView(tvEmptyState);

        renderSummary("0đ", "0", "0.0 km", getString(R.string.this_month));
        loadHistory();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        if (historyExecutor != null) {
            historyExecutor.shutdownNow();
            historyExecutor = null;
        }
    }

    private void loadHistory() {
        if (historyExecutor == null || historyExecutor.isShutdown()) {
            return;
        }

        android.content.Context appContext = requireContext().getApplicationContext();
        historyExecutor.execute(() -> {
            try {
                HistoryScreenData screenData = HistoryService.fetchHistory(appContext);
                mainHandler.post(() -> bindHistory(screenData));
            } catch (Exception exception) {
                mainHandler.post(() -> renderHistoryError(exception));
            }
        });
    }

    private void bindHistory(HistoryScreenData screenData) {
        if (!isAdded()) {
            return;
        }

        historyList.clear();
        historyList.addAll(screenData.getItems());
        adapter.notifyDataSetChanged();
        renderSummary(
                screenData.getTotalAmount(),
                screenData.getTotalTrips(),
                screenData.getTotalDistance(),
                screenData.getPeriodLabel()
        );
    }

    private void renderSummary(String totalAmount, String totalTrips, String totalDistance, String periodLabel) {
        tvSummarySub.setText(periodLabel);
        tvSummaryAmount.setText(totalAmount);
        tvTotalTrips.setText(totalTrips);
        tvTotalDistance.setText(totalDistance);
    }

    private void renderHistoryError(Exception exception) {
        if (!isAdded()) {
            return;
        }

        historyList.clear();
        adapter.notifyDataSetChanged();
        renderSummary("0đ", "0", "0.0 km", "Tháng này");

        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = "Không tải được lịch sử";
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
