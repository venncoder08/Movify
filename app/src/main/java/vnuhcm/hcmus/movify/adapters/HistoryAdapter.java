package vnuhcm.hcmus.movify.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.History;

public class HistoryAdapter extends BaseAdapter {

    private static final String TAG = "HistoryAdapter";
    private static final String DEFAULT_COMMENT_TEXT = "Chưa có nhận xét";

    private final Context context;
    private final List<History> historyList;
    private final boolean isDriverMode;

    public HistoryAdapter(Context context, List<History> historyList, boolean isDriverMode) {
        this.context = context;
        this.historyList = historyList;
        this.isDriverMode = isDriverMode;
    }

    @Override
    public int getCount() {
        return historyList.size();
    }

    @Override
    public Object getItem(int position) {
        return historyList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return historyList.get(position).getTripId();
    }

    static class ViewHolder {
        ImageView imgVehicle;
        TextView tvTripId;
        TextView tvStatus;
        TextView tvDateTime;
        TextView tvAmountLabel;
        TextView tvPrice;
        TextView tvRating;
        TextView tvMyComment;
        TextView tvPickupLabel;
        TextView tvPickupAddress;
        TextView tvDestinationLabel;
        TextView tvDestinationAddress;
        TextView tvDriverInfo;
        TextView tvDuration;
        TextView tvDistance;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);

            holder = new ViewHolder();
            holder.imgVehicle = convertView.findViewById(R.id.imgVehicle);
            holder.tvTripId = convertView.findViewById(R.id.tvTripId);
            holder.tvStatus = convertView.findViewById(R.id.tvStatus);
            holder.tvDateTime = convertView.findViewById(R.id.tvDateTime);
            holder.tvAmountLabel = convertView.findViewById(R.id.tvAmountLabel);
            holder.tvPrice = convertView.findViewById(R.id.tvPrice);
            holder.tvRating = convertView.findViewById(R.id.tvRating);
            holder.tvMyComment = convertView.findViewById(R.id.tvMyComment);
            holder.tvPickupLabel = convertView.findViewById(R.id.tvPickupLabel);
            holder.tvPickupAddress = convertView.findViewById(R.id.tvPickupAddress);
            holder.tvDestinationLabel = convertView.findViewById(R.id.tvDestinationLabel);
            holder.tvDestinationAddress = convertView.findViewById(R.id.tvDestinationAddress);
            holder.tvDriverInfo = convertView.findViewById(R.id.tvDriverInfo);
            holder.tvDuration = convertView.findViewById(R.id.tvDuration);
            holder.tvDistance = convertView.findViewById(R.id.tvDistance);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        History item = historyList.get(position);

        holder.imgVehicle.setImageResource(item.getVehicleIconResId());
        holder.tvTripId.setText("#" + item.getTripId());
        holder.tvStatus.setText(item.getStatusLabel());
        holder.tvDateTime.setText(item.getDateTime());
        holder.tvAmountLabel.setText(isDriverMode
                ? R.string.history_amount_label_driver
                : R.string.history_amount_label_user);
        holder.tvPrice.setText(item.getPrice());
        holder.tvPickupLabel.setText(isDriverMode
                ? R.string.history_pickup_label_driver
                : R.string.history_pickup_label_user);
        holder.tvPickupAddress.setText(item.getPickupAddress());
        holder.tvDestinationLabel.setText(isDriverMode
                ? R.string.history_destination_label_driver
                : R.string.history_destination_label_user);
        holder.tvDestinationAddress.setText(item.getDestinationAddress());
        if (isDriverMode) {
            holder.tvDriverInfo.setVisibility(View.GONE);
        } else {
            holder.tvDriverInfo.setVisibility(View.VISIBLE);
            holder.tvDriverInfo.setText(item.getDriverName());
        }
        holder.tvDuration.setText(item.getDuration());
        holder.tvDistance.setText(item.getDistance());

        if ("Completed".equals(item.getStatusCode())) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_success);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
        } else if ("Cancelled".equals(item.getStatusCode())) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_cancel);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_input);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.primary));
        }

        if (item.getRatingText() == null || item.getRatingText().trim().isEmpty()) {
            holder.tvRating.setVisibility(View.GONE);
            holder.tvRating.setText("");
        } else {
            holder.tvRating.setVisibility(View.VISIBLE);
            holder.tvRating.setText(item.getRatingText());
        }

        if (item.getMyComment() == null || item.getMyComment().trim().isEmpty()) {
            holder.tvMyComment.setVisibility(View.VISIBLE);
            holder.tvMyComment.setText(DEFAULT_COMMENT_TEXT);
        } else {
            holder.tvMyComment.setVisibility(View.VISIBLE);
            holder.tvMyComment.setText(item.getMyComment());
        }

        Log.d(TAG, "Bind history item: tripId=" + item.getTripId()
                + ", ratingText=" + item.getRatingText()
                + ", myComment=" + item.getMyComment()
                + ", ratingVisible=" + (holder.tvRating.getVisibility() == View.VISIBLE)
                + ", commentVisible=" + (holder.tvMyComment.getVisibility() == View.VISIBLE));

        return convertView;
    }
}
