package vnuhcm.hcmus.movify.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.Notification;

public class NotificationAdapter extends BaseAdapter {

    private final Context context;
    private final List<Notification> notificationList;

    public NotificationAdapter(Context context, List<Notification> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @Override
    public int getCount() {
        return notificationList.size();
    }

    @Override
    public Object getItem(int position) {
        return notificationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView imgIcon;
        View viewUnreadDot;
        TextView tvTitle;
        TextView tvMessage;
        TextView tvTime;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);

            holder = new ViewHolder();
            holder.imgIcon = convertView.findViewById(R.id.imgIcon);
            holder.viewUnreadDot = convertView.findViewById(R.id.viewUnreadDot);
            holder.tvTitle = convertView.findViewById(R.id.tvTitle);
            holder.tvMessage = convertView.findViewById(R.id.tvMessage);
            holder.tvTime = convertView.findViewById(R.id.tvTime);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Notification item = notificationList.get(position);

        holder.imgIcon.setImageResource(item.getIconResId());
        holder.tvTitle.setText(item.getTitle());
        holder.tvMessage.setText(item.getMessage());
        holder.tvTime.setText(item.getTime());

        holder.viewUnreadDot.setVisibility(item.isUnread() ? View.VISIBLE : View.GONE);

        if (item.isWarning()) {
            holder.imgIcon.setColorFilter(Color.parseColor("#FF3B30"), PorterDuff.Mode.SRC_IN);
        } else {
            holder.imgIcon.setColorFilter(Color.parseColor("#0D56C5"), PorterDuff.Mode.SRC_IN);
        }

        return convertView;
    }
}