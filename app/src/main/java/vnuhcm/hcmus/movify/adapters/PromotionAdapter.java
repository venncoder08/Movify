package vnuhcm.hcmus.movify.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.Promotion;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.PromotionViewHolder> {

    public interface OnPromotionClickListener {
        void onPromotionApplied(Promotion promotion);
    }

    private final List<Promotion> promotionList;
    private final OnPromotionClickListener listener;
    private int selectedPosition = -1;

    public PromotionAdapter(List<Promotion> promotionList, OnPromotionClickListener listener) {
        this.promotionList = promotionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promotion, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        Promotion promotion = promotionList.get(position);

        holder.tvPromoTitle.setText(promotion.getTitle());
        holder.tvPromoDesc.setText(promotion.getDescription());

        if (position == selectedPosition) {
            holder.btnApplyPromo.setText("Đã chọn");
            holder.btnApplyPromo.setEnabled(false);
            holder.btnApplyPromo.setBackgroundTintList(ColorStateList.valueOf(0xFF0D5BD7));
            holder.btnApplyPromo.setTextColor(0xFFFFFFFF);
        } else {
            holder.btnApplyPromo.setText("Áp dụng");
            holder.btnApplyPromo.setEnabled(true);
            holder.btnApplyPromo.setBackgroundTintList(ColorStateList.valueOf(0xFFF2F5FB));
            holder.btnApplyPromo.setTextColor(0xFF0D5BD7);
        }

        holder.btnApplyPromo.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();

            if (oldPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPosition);
            }
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onPromotionApplied(promotion);
            }
        });
    }

    @Override
    public int getItemCount() {
        return promotionList.size();
    }

    static class PromotionViewHolder extends RecyclerView.ViewHolder {
        TextView tvPromoTitle;
        TextView tvPromoDesc;
        AppCompatButton btnApplyPromo;

        public PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPromoTitle = itemView.findViewById(R.id.tvPromoTitle);
            tvPromoDesc = itemView.findViewById(R.id.tvPromoDesc);
            btnApplyPromo = itemView.findViewById(R.id.btnApplyPromo);
        }
    }
}