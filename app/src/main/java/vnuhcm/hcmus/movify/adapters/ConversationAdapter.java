package vnuhcm.hcmus.movify.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.ConversationSummary;
import vnuhcm.hcmus.movify.utils.RemoteImageLoader;

public class ConversationAdapter extends BaseAdapter {

    private final Context context;
    private final List<ConversationSummary> items;

    public ConversationAdapter(Context context, List<ConversationSummary> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getParticipant().getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ConversationSummary item = items.get(position);
        holder.tvName.setText(TextUtils.isEmpty(item.getParticipant().getFullName())
                ? context.getString(R.string.message_unknown_user)
                : item.getParticipant().getFullName());
        holder.tvPreview.setText(TextUtils.isEmpty(item.getLastMessagePreview())
                ? context.getString(R.string.message_no_messages)
                : item.getLastMessagePreview());
        holder.tvTime.setText(formatTime(item.getUpdatedAt()));
        RemoteImageLoader.loadCircularAvatar(holder.ivAvatar, item.getParticipant().getAvatar());

        if (item.getUnreadCount() > 0) {
            holder.tvUnread.setVisibility(View.VISIBLE);
            holder.tvUnread.setText(formatUnreadCount(item.getUnreadCount()));
        } else {
            holder.tvUnread.setVisibility(View.GONE);
        }

        holder.tvActive.setVisibility(item.getActiveTripId() != null ? View.VISIBLE : View.GONE);
        holder.tvReadOnly.setVisibility(item.canSend() ? View.GONE : View.VISIBLE);

        return convertView;
    }

    private String formatTime(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "";
        }

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX"
        };
        for (String pattern : patterns) {
            try {
                Date date = new SimpleDateFormat(pattern, Locale.US).parse(raw);
                if (date != null) {
                    return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
                }
            } catch (ParseException ignored) {
            }
        }
        return "";
    }

    private String formatUnreadCount(int unreadCount) {
        return unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
    }

    private static class ViewHolder {
        final ImageView ivAvatar;
        final TextView tvName;
        final TextView tvPreview;
        final TextView tvTime;
        final TextView tvUnread;
        final TextView tvActive;
        final TextView tvReadOnly;

        ViewHolder(View itemView) {
            ivAvatar = itemView.findViewById(R.id.ivConversationAvatar);
            tvName = itemView.findViewById(R.id.tvConversationName);
            tvPreview = itemView.findViewById(R.id.tvConversationPreview);
            tvTime = itemView.findViewById(R.id.tvConversationTime);
            tvUnread = itemView.findViewById(R.id.tvConversationUnread);
            tvActive = itemView.findViewById(R.id.tvConversationActive);
            tvReadOnly = itemView.findViewById(R.id.tvConversationReadOnly);
        }
    }
}
