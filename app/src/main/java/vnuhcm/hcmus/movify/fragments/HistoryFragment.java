package vnuhcm.hcmus.movify.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

    private static final String ARG_IS_DRIVER_MODE = "is_driver_mode";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ListView lvHistory;
    private ImageView imgSummaryIcon;
    private TextView tvTitle;
    private TextView tvSummaryTitle;
    private TextView tvSummarySub;
    private TextView tvSummaryAmount;
    private TextView tvTotalTripsLabel;
    private TextView tvTotalTrips;
    private TextView tvTotalDistanceLabel;
    private TextView tvTotalDistance;
    private TextView tvEmptyState;
    private HistoryAdapter adapter;
    private final List<History> historyList = new ArrayList<>();
    private ExecutorService historyExecutor;
    private boolean isDriverMode;

    public HistoryFragment() {
    }

    public static HistoryFragment newInstance(boolean isDriverMode) {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_DRIVER_MODE, isDriverMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            isDriverMode = getArguments().getBoolean(ARG_IS_DRIVER_MODE, false);
        }

        View view = inflater.inflate(R.layout.fragment_history, container, false);

        lvHistory = view.findViewById(R.id.lvHistory);
        imgSummaryIcon = view.findViewById(R.id.imgSummaryIcon);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvSummaryTitle = view.findViewById(R.id.tvSummaryTitle);
        tvSummarySub = view.findViewById(R.id.tvSummarySub);
        tvSummaryAmount = view.findViewById(R.id.tvSummaryAmount);
        tvTotalTripsLabel = view.findViewById(R.id.tvTotalTripsLabel);
        tvTotalTrips = view.findViewById(R.id.tvTotalTrips);
        tvTotalDistanceLabel = view.findViewById(R.id.tvTotalDistanceLabel);
        tvTotalDistance = view.findViewById(R.id.tvTotalDistance);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        historyExecutor = Executors.newSingleThreadExecutor();
        adapter = new HistoryAdapter(requireContext(), historyList, isDriverMode);
        lvHistory.setAdapter(adapter);
        lvHistory.setEmptyView(tvEmptyState);

        applyCopyByRole();
        renderHistoryLoading();
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

        applyEmptyStateByRole();
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

    private void renderHistoryLoading() {
        String loadingText = getString(R.string.loading_dots);
        tvEmptyState.setText(loadingText);
        renderSummary(loadingText, loadingText, loadingText, loadingText);
    }

    private void renderHistoryError(Exception exception) {
        if (!isAdded()) {
            return;
        }

        applyEmptyStateByRole();
        historyList.clear();
        adapter.notifyDataSetChanged();
        renderSummary("0đ", "0", "0.0 km", "Tháng này");

        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = "Không tải được lịch sử";
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void applyCopyByRole() {
        tvTitle.setText(isDriverMode ? R.string.history_title_driver : R.string.history_title_user);
        tvSummaryTitle.setText(isDriverMode ? R.string.history_summary_revenue : R.string.history_summary_spending);
        tvTotalTripsLabel.setText(isDriverMode ? R.string.history_total_trips_driver : R.string.history_total_trips_user);
        tvTotalDistanceLabel.setText(R.string.history_total_distance);
        applyEmptyStateByRole();
        imgSummaryIcon.setImageResource(isDriverMode ? R.drawable.ic_money : R.drawable.ic_card);
    }

    private void applyEmptyStateByRole() {
        tvEmptyState.setText(isDriverMode ? R.string.history_empty_driver : R.string.history_empty_user);
    }
}
