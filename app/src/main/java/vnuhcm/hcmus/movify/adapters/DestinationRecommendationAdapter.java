package vnuhcm.hcmus.movify.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.DestinationRecommendation;

public class DestinationRecommendationAdapter
        extends RecyclerView.Adapter<DestinationRecommendationAdapter.RecommendationViewHolder> {

    public interface OnRecommendationClickListener {
        void onRecommendationSelected(DestinationRecommendation recommendation);
    }

    private final List<DestinationRecommendation> recommendations;
    private final OnRecommendationClickListener listener;

    public DestinationRecommendationAdapter(List<DestinationRecommendation> recommendations,
                                            OnRecommendationClickListener listener) {
        this.recommendations = recommendations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_destination_recommendation, parent, false);
        return new RecommendationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        DestinationRecommendation recommendation = recommendations.get(position);
        holder.tvName.setText(recommendation.getName());
        holder.tvAddress.setText(recommendation.getAddress());
        holder.tvMeta.setText(buildMetaText(recommendation));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecommendationSelected(recommendation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    @NonNull
    private static String buildMetaText(@NonNull DestinationRecommendation recommendation) {
        return recommendation.getCategoryLabel()
                + " - "
                + formatDistance(recommendation.getDistanceMeters());
    }

    @NonNull
    private static String formatDistance(double distanceMeters) {
        if (distanceMeters >= 1000d) {
            return String.format(Locale.getDefault(), "%.1f km", distanceMeters / 1000d);
        }

        return Math.max(1, Math.round(distanceMeters)) + " m";
    }

    static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvAddress;
        TextView tvMeta;

        RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRecommendationName);
            tvAddress = itemView.findViewById(R.id.tvRecommendationAddress);
            tvMeta = itemView.findViewById(R.id.tvRecommendationMeta);
        }
    }
}
