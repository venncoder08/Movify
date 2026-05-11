package vnuhcm.hcmus.movify.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.ChatMessage;
import vnuhcm.hcmus.movify.utils.RemoteImageLoader;

public class ChatMessageAdapter extends BaseAdapter {

    private final Context context;
    private final List<ChatMessage> messages;

    public ChatMessageAdapter(Context context, List<ChatMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return messages.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ChatMessage message = messages.get(position);
        boolean isMine = message.isMine();

        holder.outgoingContainer.setVisibility(isMine ? View.VISIBLE : View.GONE);
        holder.incomingContainer.setVisibility(isMine ? View.GONE : View.VISIBLE);

        bindBubble(
                isMine ? holder.tvOutgoingMessage : holder.tvIncomingMessage,
                isMine ? holder.ivOutgoingImage : holder.ivIncomingImage,
                isMine ? holder.tvOutgoingTime : holder.tvIncomingTime,
                message
        );

        return convertView;
    }

    private void bindBubble(TextView messageView, ImageView imageView, TextView timeView, ChatMessage message) {
        if (message.isImage() && !TextUtils.isEmpty(message.getMediaUrl())) {
            imageView.setVisibility(View.VISIBLE);
            RemoteImageLoader.loadImage(imageView, message.getMediaUrl(), R.drawable.ic_message);
        } else {
            imageView.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(message.getContent())) {
            messageView.setVisibility(View.GONE);
        } else {
            messageView.setVisibility(View.VISIBLE);
            messageView.setText(message.getContent());
        }

        timeView.setText(formatTime(message.getCreatedAt()));
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

    private static class ViewHolder {
        final LinearLayout incomingContainer;
        final LinearLayout outgoingContainer;
        final ImageView ivIncomingImage;
        final ImageView ivOutgoingImage;
        final TextView tvIncomingMessage;
        final TextView tvOutgoingMessage;
        final TextView tvIncomingTime;
        final TextView tvOutgoingTime;

        ViewHolder(View itemView) {
            incomingContainer = itemView.findViewById(R.id.containerIncoming);
            outgoingContainer = itemView.findViewById(R.id.containerOutgoing);
            ivIncomingImage = itemView.findViewById(R.id.ivIncomingImage);
            ivOutgoingImage = itemView.findViewById(R.id.ivOutgoingImage);
            tvIncomingMessage = itemView.findViewById(R.id.tvIncomingMessage);
            tvOutgoingMessage = itemView.findViewById(R.id.tvOutgoingMessage);
            tvIncomingTime = itemView.findViewById(R.id.tvIncomingTime);
            tvOutgoingTime = itemView.findViewById(R.id.tvOutgoingTime);
        }
    }
}
