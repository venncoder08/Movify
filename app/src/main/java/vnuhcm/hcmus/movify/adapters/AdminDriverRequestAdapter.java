package vnuhcm.hcmus.movify.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.DriverRequest;
import vnuhcm.hcmus.movify.utils.RemoteImageLoader;

public class AdminDriverRequestAdapter extends RecyclerView.Adapter<AdminDriverRequestAdapter.DriverRequestViewHolder> {

    public interface Listener {
        void onApprove(DriverRequest request);

        void onReject(DriverRequest request);

        void onDocumentClicked(String title, String imageUrl);
    }

    private final List<DriverRequest> requests;
    private final Listener listener;

    public AdminDriverRequestAdapter(List<DriverRequest> requests, Listener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DriverRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_driver_request, parent, false);
        return new DriverRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverRequestViewHolder holder, int position) {
        DriverRequest request = requests.get(position);
        Context context = holder.itemView.getContext();

        holder.tvDriverName.setText(firstNonEmpty(
                request.getDriverName(),
                context.getString(R.string.admin_driver_request_user_prefix, request.getId())
        ));
        holder.tvDriverMeta.setText(buildDriverMeta(context, request));
        holder.tvRequestDate.setText(context.getString(
                R.string.admin_driver_request_sent_date,
                formatDate(request.getCreatedAt())
        ));
        holder.tvRequestStatus.setText(mapStatus(context, request.getStatus()));
        holder.tvVehicleType.setText(mapVehicleType(context, request.getVehicleType()));

        RemoteImageLoader.loadCircularAvatar(holder.ivDriverAvatar, request.getDriverAvatar());
        RemoteImageLoader.loadImage(holder.ivIdCardImage, request.getIdCardImageUrl(), R.drawable.ic_card);
        RemoteImageLoader.loadImage(holder.ivDriverLicenseImage, request.getDriverLicenseImageUrl(), R.drawable.ic_card);
        RemoteImageLoader.loadImage(holder.ivVehicleRegistrationImage, request.getVehicleRegistrationImageUrl(), R.drawable.ic_card);

        holder.ivIdCardImage.setOnClickListener(v ->
                notifyDocumentClicked(context.getString(R.string.admin_driver_request_id_card_title), request.getIdCardImageUrl()));
        holder.ivDriverLicenseImage.setOnClickListener(v ->
                notifyDocumentClicked(context.getString(R.string.admin_driver_request_license_title), request.getDriverLicenseImageUrl()));
        holder.ivVehicleRegistrationImage.setOnClickListener(v ->
                notifyDocumentClicked(context.getString(R.string.admin_driver_request_registration_title), request.getVehicleRegistrationImageUrl()));

        holder.actionsContainer.setVisibility(request.isPending() ? View.VISIBLE : View.GONE);
        holder.btnApproveRequest.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApprove(request);
            }
        });
        holder.btnRejectRequest.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    private void notifyDocumentClicked(String title, String imageUrl) {
        if (listener != null) {
            listener.onDocumentClicked(title, imageUrl);
        }
    }

    private String buildDriverMeta(Context context, DriverRequest request) {
        if (!TextUtils.isEmpty(request.getDriverPhoneNumber()) && !TextUtils.isEmpty(request.getDriverEmail())) {
            return request.getDriverPhoneNumber() + " - " + request.getDriverEmail();
        }

        return firstNonEmpty(
                request.getDriverPhoneNumber(),
                request.getDriverEmail(),
                context.getString(R.string.admin_driver_request_missing_contact)
        );
    }

    private String mapStatus(Context context, String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            return context.getString(R.string.admin_driver_request_approved_status);
        }

        if ("Rejected".equalsIgnoreCase(status)) {
            return context.getString(R.string.admin_driver_request_rejected_status);
        }

        return context.getString(R.string.admin_driver_request_pending_status);
    }

    private String mapVehicleType(Context context, String vehicleType) {
        if ("Taxi".equalsIgnoreCase(vehicleType) || "car".equalsIgnoreCase(vehicleType)) {
            return context.getString(R.string.admin_driver_request_vehicle_taxi);
        }

        if ("Motor".equalsIgnoreCase(vehicleType) || "motorbike".equalsIgnoreCase(vehicleType)) {
            return context.getString(R.string.admin_driver_request_vehicle_motor);
        }

        return context.getString(R.string.admin_driver_request_vehicle_unknown);
    }

    private String formatDate(String rawDate) {
        if (TextUtils.isEmpty(rawDate)) {
            return "";
        }

        int separatorIndex = rawDate.indexOf('T');
        if (separatorIndex > 0) {
            return rawDate.substring(0, separatorIndex);
        }

        return rawDate;
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    static class DriverRequestViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDriverAvatar;
        ImageView ivIdCardImage;
        ImageView ivDriverLicenseImage;
        ImageView ivVehicleRegistrationImage;
        LinearLayout actionsContainer;
        TextView tvDriverName;
        TextView tvDriverMeta;
        TextView tvRequestStatus;
        TextView tvRequestDate;
        TextView tvVehicleType;
        TextView btnApproveRequest;
        TextView btnRejectRequest;

        DriverRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDriverAvatar = itemView.findViewById(R.id.ivDriverAvatar);
            ivIdCardImage = itemView.findViewById(R.id.ivIdCardImage);
            ivDriverLicenseImage = itemView.findViewById(R.id.ivDriverLicenseImage);
            ivVehicleRegistrationImage = itemView.findViewById(R.id.ivVehicleRegistrationImage);
            actionsContainer = itemView.findViewById(R.id.actionsContainer);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvDriverMeta = itemView.findViewById(R.id.tvDriverMeta);
            tvRequestStatus = itemView.findViewById(R.id.tvRequestStatus);
            tvRequestDate = itemView.findViewById(R.id.tvRequestDate);
            tvVehicleType = itemView.findViewById(R.id.tvVehicleType);
            btnApproveRequest = itemView.findViewById(R.id.btnApproveRequest);
            btnRejectRequest = itemView.findViewById(R.id.btnRejectRequest);
        }
    }
}
