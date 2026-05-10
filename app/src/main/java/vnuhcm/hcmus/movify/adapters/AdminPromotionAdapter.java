package vnuhcm.hcmus.movify.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.Promotion;

public class AdminPromotionAdapter extends RecyclerView.Adapter<AdminPromotionAdapter.PromotionViewHolder> {

    private final List<Promotion> promotions;
    private OnActionListener actionListener;

    public interface OnActionListener {
        void onEdit(Promotion promotion);
        void onDelete(Promotion promotion);
    }

    public void setOnActionListener(OnActionListener listener) {
        this.actionListener = listener;
    }

    public AdminPromotionAdapter(List<Promotion> promotions) {
        this.promotions = promotions;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_promotion, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        Promotion promotion = promotions.get(position);
        Context context = holder.itemView.getContext();

        holder.tvPromotionCode.setText(promotion.getTitle());
        holder.tvPromotionDescription.setText(promotion.getDescription());
        holder.tvPromotionType.setText(mapPromotionType(context, promotion.getType()));
        holder.tvPromotionValue.setText(formatDiscountValue(promotion.getType(), promotion.getDiscountValue()));

        holder.btnEdit.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onEdit(promotion);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDelete(promotion);
        });
    }

    @Override
    public int getItemCount() {
        return promotions.size();
    }

    private String formatDiscountValue(String type, double value) {
        if (value == 0) {
            return "0";
        }

        if ("percent".equalsIgnoreCase(type)) {
            return formatNumber(value) + "%";
        }

        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);
        return formatter.format(value) + "đ";
    }

    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }

        return String.valueOf(value);
    }

    private String mapPromotionType(Context context, String type) {
        if ("percent".equalsIgnoreCase(type)) {
            return context.getString(R.string.admin_promotion_type_percent);
        }

        if ("vnd".equalsIgnoreCase(type)) {
            return context.getString(R.string.admin_promotion_type_vnd);
        }

        return context.getString(R.string.admin_promotion_type_unknown);
    }

    static class PromotionViewHolder extends RecyclerView.ViewHolder {
        TextView tvPromotionCode;
        TextView tvPromotionDescription;
        TextView tvPromotionType;
        TextView tvPromotionValue;

        PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPromotionCode = itemView.findViewById(R.id.tvPromotionCode);
            tvPromotionDescription = itemView.findViewById(R.id.tvPromotionDescription);
            tvPromotionType = itemView.findViewById(R.id.tvPromotionType);
            tvPromotionValue = itemView.findViewById(R.id.tvPromotionValue);
            btnEdit = itemView.findViewById(R.id.btnPromotionEdit);
            btnDelete = itemView.findViewById(R.id.btnPromotionDelete);
        }
        TextView btnEdit;
        TextView btnDelete;
    }
}
