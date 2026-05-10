package vnuhcm.hcmus.movify.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.adapters.AdminDriverRequestAdapter;
import vnuhcm.hcmus.movify.models.DriverRequest;
import vnuhcm.hcmus.movify.services.DriverRequestService;
import vnuhcm.hcmus.movify.utils.RemoteImageLoader;

public class AdminRequestFragment extends Fragment {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<DriverRequest> driverRequests = new ArrayList<>();

    private ExecutorService executorService;
    private AdminDriverRequestAdapter adapter;
    private RecyclerView rvAdminDriverRequests;
    private ProgressBar progressAdminRequests;
    private TextView tvAdminRequestEmpty;
    private TextView btnAdminRequestRefresh;

    public AdminRequestFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_request, container, false);
        executorService = Executors.newSingleThreadExecutor();
        initViews(view);
        loadDriverRequests();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    private void initViews(View view) {
        rvAdminDriverRequests = view.findViewById(R.id.rvAdminDriverRequests);
        progressAdminRequests = view.findViewById(R.id.progressAdminRequests);
        tvAdminRequestEmpty = view.findViewById(R.id.tvAdminRequestEmpty);
        btnAdminRequestRefresh = view.findViewById(R.id.btnAdminRequestRefresh);

        adapter = new AdminDriverRequestAdapter(driverRequests, new AdminDriverRequestAdapter.Listener() {
            @Override
            public void onApprove(DriverRequest request) {
                confirmReview(request, "Approved");
            }

            @Override
            public void onReject(DriverRequest request) {
                confirmReview(request, "Rejected");
            }

            @Override
            public void onDocumentClicked(String title, String imageUrl) {
                showDocumentPreview(title, imageUrl);
            }
        });

        rvAdminDriverRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAdminDriverRequests.setAdapter(adapter);
        btnAdminRequestRefresh.setOnClickListener(v -> loadDriverRequests());
    }

    private void loadDriverRequests() {
        if (executorService == null || executorService.isShutdown() || !isAdded()) {
            return;
        }

        Context context = requireContext().getApplicationContext();
        setLoading(true);
        executorService.execute(() -> {
            try {
                List<DriverRequest> loadedRequests = DriverRequestService.fetchAdminDriverRequests(context, "");
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    driverRequests.clear();
                    driverRequests.addAll(loadedRequests);
                    adapter.notifyDataSetChanged();
                    setLoading(false);
                    updateEmptyState();
                });
            } catch (Exception exception) {
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    setLoading(false);
                    updateEmptyState();
                    Toast.makeText(requireContext(),
                            resolveMessage(exception, getString(R.string.admin_driver_request_load_error)),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void confirmReview(DriverRequest request, String status) {
        String title = "Approved".equals(status)
                ? getString(R.string.admin_driver_request_confirm_approve_title)
                : getString(R.string.admin_driver_request_confirm_reject_title);
        String message = "Approved".equals(status)
                ? getString(R.string.admin_driver_request_confirm_approve_message)
                : getString(R.string.admin_driver_request_confirm_reject_message);

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.admin_driver_request_confirm, (dialog, which) -> reviewRequest(request, status))
                .setNegativeButton(R.string.admin_driver_request_cancel, null)
                .show();
    }

    private void reviewRequest(DriverRequest request, String status) {
        if (executorService == null || executorService.isShutdown() || !isAdded()) {
            return;
        }

        Context context = requireContext().getApplicationContext();
        setLoading(true);
        executorService.execute(() -> {
            try {
                String message = DriverRequestService.updateDriverRequestStatus(context, request.getId(), status);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (!TextUtils.isEmpty(message)) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                    loadDriverRequests();
                });
            } catch (Exception exception) {
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    setLoading(false);
                    Toast.makeText(requireContext(),
                            resolveMessage(exception, getString(R.string.admin_driver_request_update_error)),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showDocumentPreview(String title, String imageUrl) {
        if (TextUtils.isEmpty(imageUrl) || !isAdded()) {
            Toast.makeText(requireContext(), R.string.admin_driver_request_no_document, Toast.LENGTH_SHORT).show();
            return;
        }

        ImageView imageView = new ImageView(requireContext());
        int padding = Math.round(12 * getResources().getDisplayMetrics().density);
        imageView.setPadding(padding, padding, padding, padding);
        imageView.setAdjustViewBounds(true);
        imageView.setMinimumHeight(Math.round(320 * getResources().getDisplayMetrics().density));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        RemoteImageLoader.loadImage(imageView, imageUrl, R.drawable.ic_card);

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(imageView)
                .setPositiveButton(R.string.admin_driver_request_close, null)
                .show();
    }

    private void setLoading(boolean loading) {
        progressAdminRequests.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnAdminRequestRefresh.setEnabled(!loading);
        btnAdminRequestRefresh.setAlpha(loading ? 0.55f : 1f);
    }

    private void updateEmptyState() {
        boolean isEmpty = driverRequests.isEmpty();
        tvAdminRequestEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvAdminDriverRequests.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private String resolveMessage(Exception exception, String fallbackMessage) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return fallbackMessage;
        }

        return message;
    }
}
