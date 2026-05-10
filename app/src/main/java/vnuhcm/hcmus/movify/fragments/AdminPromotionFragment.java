package vnuhcm.hcmus.movify.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.adapters.AdminPromotionAdapter;
import vnuhcm.hcmus.movify.models.Promotion;
import vnuhcm.hcmus.movify.services.PromotionService;

public class AdminPromotionFragment extends Fragment implements AdminPromotionDialogFragment.Listener {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Promotion> promotions = new ArrayList<>();

    private ExecutorService executorService;
    private AdminPromotionAdapter adapter;
    private RecyclerView rvAdminPromotions;
    private ProgressBar progressAdminPromotions;
    private TextView tvAdminPromotionEmpty;
    private TextView btnAdminPromotionRefresh;
    private TextView btnAdminPromotionCreate;

    public AdminPromotionFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_promotion, container, false);
        executorService = Executors.newSingleThreadExecutor();
        initViews(view);
        loadPromotions();
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
        rvAdminPromotions = view.findViewById(R.id.rvAdminPromotions);
        progressAdminPromotions = view.findViewById(R.id.progressAdminPromotions);
        tvAdminPromotionEmpty = view.findViewById(R.id.tvAdminPromotionEmpty);
        btnAdminPromotionRefresh = view.findViewById(R.id.btnAdminPromotionRefresh);
        btnAdminPromotionCreate = view.findViewById(R.id.btnAdminPromotionCreate);

        adapter = new AdminPromotionAdapter(promotions);
        rvAdminPromotions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAdminPromotions.setAdapter(adapter);
        btnAdminPromotionRefresh.setOnClickListener(v -> loadPromotions());
        btnAdminPromotionCreate.setOnClickListener(v -> openCreateDialog());
        btnAdminPromotionRefresh.setOnLongClickListener(v -> {
            openCreateDialog();
            return true;
        });

        adapter.setOnActionListener(new AdminPromotionAdapter.OnActionListener() {
            @Override
            public void onEdit(vnuhcm.hcmus.movify.models.Promotion promotion) {
                openEditDialog(promotion);
            }

            @Override
            public void onDelete(vnuhcm.hcmus.movify.models.Promotion promotion) {
                confirmAndDelete(promotion);
            }
        });
    }

    private void openCreateDialog() {
        AdminPromotionDialogFragment dialog = AdminPromotionDialogFragment.newInstance(null);
        dialog.setListener(this);
        dialog.show(getParentFragmentManager(), "promo_create");
    }

    private void openEditDialog(vnuhcm.hcmus.movify.models.Promotion promotion) {
        AdminPromotionDialogFragment dialog = AdminPromotionDialogFragment.newInstance(promotion);
        dialog.setListener(this);
        dialog.show(getParentFragmentManager(), "promo_edit");
    }

    private void confirmAndDelete(vnuhcm.hcmus.movify.models.Promotion promotion) {
        if (executorService == null) return;
        executorService.execute(() -> {
            try {
                PromotionService.deletePromotion(requireContext(), promotion.getId());
                mainHandler.post(() -> {
                    Toast.makeText(requireContext(), R.string.admin_promotion_delete_success, Toast.LENGTH_SHORT).show();
                    loadPromotions();
                });
            } catch (Exception ex) {
                mainHandler.post(() -> Toast.makeText(requireContext(), ex.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadPromotions() {
        if (executorService == null || executorService.isShutdown() || !isAdded()) {
            return;
        }

        renderLoading();
        Context appContext = requireContext().getApplicationContext();
        executorService.execute(() -> {
            try {
                List<Promotion> loadedPromotions = PromotionService.fetchPromotions(appContext);
                mainHandler.post(() -> bindPromotions(loadedPromotions));
            } catch (Exception exception) {
                mainHandler.post(() -> renderError(exception));
            }
        });
    }

    private void bindPromotions(List<Promotion> loadedPromotions) {
        if (!isAdded()) {
            return;
        }

        progressAdminPromotions.setVisibility(View.GONE);
        rvAdminPromotions.setVisibility(loadedPromotions.isEmpty() ? View.GONE : View.VISIBLE);
        tvAdminPromotionEmpty.setVisibility(loadedPromotions.isEmpty() ? View.VISIBLE : View.GONE);
        btnAdminPromotionRefresh.setEnabled(true);
        btnAdminPromotionRefresh.setAlpha(1f);
        btnAdminPromotionCreate.setEnabled(true);
        btnAdminPromotionCreate.setAlpha(1f);

        if (loadedPromotions.isEmpty()) {
            tvAdminPromotionEmpty.setText(R.string.admin_promotion_empty);
        }

        promotions.clear();
        promotions.addAll(loadedPromotions);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSavePromotion(Promotion promotion) {
        if (executorService == null) return;
        executorService.execute(() -> {
            try {
                if (promotion.getId() == 0) {
                    PromotionService.createPromotion(requireContext(), promotion);
                } else {
                    PromotionService.updatePromotion(requireContext(), promotion);
                }

                mainHandler.post(() -> {
                    Toast.makeText(requireContext(), R.string.admin_promotion_save_success, Toast.LENGTH_SHORT).show();
                    loadPromotions();
                });
            } catch (Exception ex) {
                mainHandler.post(() -> Toast.makeText(requireContext(), ex.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void renderLoading() {
        if (!isAdded()) {
            return;
        }

        progressAdminPromotions.setVisibility(View.VISIBLE);
        rvAdminPromotions.setVisibility(View.GONE);
        tvAdminPromotionEmpty.setVisibility(View.GONE);
        tvAdminPromotionEmpty.setText(R.string.loading_dots);
        btnAdminPromotionRefresh.setEnabled(false);
        btnAdminPromotionRefresh.setAlpha(0.55f);
        btnAdminPromotionCreate.setEnabled(false);
        btnAdminPromotionCreate.setAlpha(0.55f);
    }

    private void renderError(Exception exception) {
        if (!isAdded()) {
            return;
        }

        progressAdminPromotions.setVisibility(View.GONE);
        rvAdminPromotions.setVisibility(View.GONE);
        tvAdminPromotionEmpty.setVisibility(View.VISIBLE);
        btnAdminPromotionRefresh.setEnabled(true);
        btnAdminPromotionRefresh.setAlpha(1f);
        btnAdminPromotionCreate.setEnabled(true);
        btnAdminPromotionCreate.setAlpha(1f);

        String message = getString(R.string.admin_promotion_error);
        tvAdminPromotionEmpty.setText(message);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
