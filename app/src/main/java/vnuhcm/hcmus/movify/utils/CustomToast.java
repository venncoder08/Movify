package vnuhcm.hcmus.movify.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

import vnuhcm.hcmus.movify.R;

public class CustomToast {

    public enum Type {
        SUCCESS,
        ERROR,
        WARNING,
        INFO
    }

    public static void showSuccess(View parent, Context context, String message) {
        show(parent, context, Type.SUCCESS, message);
    }

    public static void showError(View parent, Context context, String message) {
        show(parent, context, Type.ERROR, message);
    }

    public static void showWarning(View parent, Context context, String message) {
        show(parent, context, Type.WARNING, message);
    }

    public static void showInfo(View parent, Context context, String message) {
        show(parent, context, Type.INFO, message);
    }

    @SuppressLint("RestrictedApi")
    public static void show(View parent, Context context, Type type, String message) {
        Snackbar snackbar = Snackbar.make(parent, "", Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();

        snackbarView.setBackgroundColor(Color.TRANSPARENT);

        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbarView;
        layout.removeAllViews();
        layout.setPadding(0, 0, 0, 0);

        View customView = LayoutInflater.from(context).inflate(R.layout.custom_snackbar, null);

        FrameLayout iconContainer = customView.findViewById(R.id.iconContainer);
        ImageView ivIcon = customView.findViewById(R.id.ivIcon);
        TextView tvTitle = customView.findViewById(R.id.tvTitle);
        TextView tvMessage = customView.findViewById(R.id.tvMessage);
        ImageButton btnClose = customView.findViewById(R.id.btnClose);

        int iconRes;
        int bgCircleRes;
        String title;

        switch (type) {
            case SUCCESS:
                iconRes = R.drawable.ic_success;
                bgCircleRes = R.drawable.bg_circle_success;
                title = "Thành công";
                break;

            case ERROR:
                iconRes = R.drawable.ic_error;
                bgCircleRes = R.drawable.bg_circle_error;
                title = "Lỗi xảy ra";
                break;

            case WARNING:
                iconRes = R.drawable.ic_warning;
                bgCircleRes = R.drawable.bg_circle_warning;
                title = "Cảnh báo";
                break;

            default:
                iconRes = R.drawable.ic_info;
                bgCircleRes = R.drawable.bg_circle_info;
                title = "Thông tin";
                break;
        }

        iconContainer.setBackgroundResource(bgCircleRes);
        ivIcon.setImageResource(iconRes);
        tvTitle.setText(title);
        tvMessage.setText(message);
        tvMessage.setMaxLines(3);

        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int width75 = (int) (screenWidth * 0.75f);

        ViewGroup.LayoutParams customParams = new ViewGroup.LayoutParams(
                width75,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        customView.setLayoutParams(customParams);

        btnClose.setOnClickListener(v -> snackbar.dismiss());

        layout.addView(customView);

        ViewGroup.LayoutParams params = snackbarView.getLayoutParams();

        if (params instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) params;
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.TOP | Gravity.END;
            lp.setMargins(dp(context, 16), dp(context, 24), dp(context, 16), 0);
            snackbarView.setLayoutParams(lp);
        } else if (params instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) params;
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.TOP | Gravity.END;
            lp.setMargins(dp(context, 16), dp(context, 24), dp(context, 16), 0);
            snackbarView.setLayoutParams(lp);
        }

        snackbar.show();
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}