package vnuhcm.hcmus.movify.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.History;

public class HistoryAdapter extends BaseAdapter {

    private final Context context;
    private final List<History> historyList;

    public HistoryAdapter(Context context, List<History> historyList) {
        this.context = context;
        this.historyList = historyList;
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
        TextView tvTripId, tvStatus, tvDateTime, tvPrice, tvRating;
        TextView tvPickupAddress, tvDestinationAddress;
        TextView tvDriverInfo, tvDuration, tvDistance;
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
            holder.tvPrice = convertView.findViewById(R.id.tvPrice);
            holder.tvRating = convertView.findViewById(R.id.tvRating);
            holder.tvPickupAddress = convertView.findViewById(R.id.tvPickupAddress);
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
        holder.tvDateTime.setText(item.getDateTime());
        holder.tvPrice.setText(item.getPrice());
        holder.tvPickupAddress.setText(item.getPickupAddress());
        holder.tvDestinationAddress.setText(item.getDestinationAddress());
        holder.tvDriverInfo.setText(item.getDriverName() + " • " + item.getDriverPlate());
        holder.tvDuration.setText(item.getDuration());
        holder.tvDistance.setText(item.getDistance());

        if ("Hoàn thành".equals(item.getStatus())) {
            holder.tvStatus.setText("Hoàn thành");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_success);
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            holder.tvRating.setVisibility(View.VISIBLE);
            holder.tvRating.setText("★ 4.8");
        } else {
            holder.tvStatus.setText("Đã hủy");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_cancel);
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            holder.tvRating.setVisibility(View.VISIBLE);
            holder.tvRating.setText("★ 4.7");
        }

        return convertView;
    }
}