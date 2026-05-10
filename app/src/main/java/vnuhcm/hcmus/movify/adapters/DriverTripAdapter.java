package vnuhcm.hcmus.movify.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.Trip;
import vnuhcm.hcmus.movify.utils.CurrentLocationResolver;

public class DriverTripAdapter extends RecyclerView.Adapter<DriverTripAdapter.TripViewHolder> {

    private List<Trip> trips;
    private OnTripSelectedListener onTripSelectedListener;
    private int selectedPosition = 0;

    public interface OnTripSelectedListener {
        void onTripSelected(Trip trip);
    }

    public DriverTripAdapter(List<Trip> trips, OnTripSelectedListener onTripSelectedListener) {
        this.trips = trips;
        this.onTripSelectedListener = onTripSelectedListener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = trips.get(position);
        holder.bind(trip, position, selectedPosition);
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    public void selectTrip(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    class TripViewHolder extends RecyclerView.ViewHolder {

        private TextView tvPickupAddress;
        private TextView tvDestinationAddress;
        private TextView tvFare;
        private TextView tvDistance;
        private LinearLayout tripContainer;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPickupAddress = itemView.findViewById(R.id.tvPickupAddress);
            tvDestinationAddress = itemView.findViewById(R.id.tvDestinationAddress);
            tvFare = itemView.findViewById(R.id.tvFare);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tripContainer = itemView.findViewById(R.id.tripContainer);
        }

        public void bind(Trip trip, int position, int selectedPosition) {
            tvPickupAddress.setText(formatTripAddress(trip.getPickupAddress(), "Chưa rõ điểm đón"));
            tvDestinationAddress.setText(formatTripAddress(trip.getDestinationAddress(), "Chưa rõ điểm đến"));
            tvFare.setText(String.format("%.2fđ", trip.getFare()));
            tvDistance.setText(String.format("%s • %.1f km", mapTripTypeLabel(trip), trip.getDistance()));

            // Highlight selected trip
            if (position == selectedPosition) {
                tripContainer.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_blue_light));
            } else {
                tripContainer.setBackgroundColor(itemView.getContext().getColor(android.R.color.white));
            }

            tripContainer.setOnClickListener(v -> {
                selectTrip(position);
                if (onTripSelectedListener != null) {
                    onTripSelectedListener.onTripSelected(trip);
                }
            });
        }

        private String mapTripTypeLabel(Trip trip) {
            if ("Taxi".equalsIgnoreCase(trip.getTripType())) {
                return itemView.getContext().getString(R.string.vehicle_car);
            }

            return itemView.getContext().getString(R.string.vehicle_bike);
        }

        private String formatTripAddress(String address, String fallback) {
            String displayAddress = CurrentLocationResolver.getShareableLocationLabel(address);
            return TextUtils.isEmpty(displayAddress) ? fallback : displayAddress;
        }
    }
}
