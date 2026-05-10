package vnuhcm.hcmus.movify.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vnuhcm.hcmus.movify.R;

public final class RemoteImageLoader {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private RemoteImageLoader() {
    }

    public static void loadAvatar(ImageView imageView, @Nullable String imageUrl) {
        imageView.setTag(imageUrl);

        if (TextUtils.isEmpty(imageUrl)) {
            applyFallback(imageView, R.drawable.ic_user, true);
            return;
        }

        EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(imageUrl);
            MAIN_HANDLER.post(() -> {
                Object currentTag = imageView.getTag();
                if (currentTag == null || !currentTag.equals(imageUrl)) {
                    return;
                }

                if (bitmap == null) {
                    applyFallback(imageView, R.drawable.ic_user, true);
                    return;
                }

                imageView.setImageBitmap(bitmap);
                imageView.setImageTintList(null);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            });
        });
    }

    public static void loadImage(ImageView imageView, @Nullable String imageUrl, @DrawableRes int fallbackRes) {
        imageView.setTag(imageUrl);

        if (TextUtils.isEmpty(imageUrl)) {
            applyFallback(imageView, fallbackRes, false);
            return;
        }

        EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(imageUrl);
            MAIN_HANDLER.post(() -> {
                Object currentTag = imageView.getTag();
                if (currentTag == null || !currentTag.equals(imageUrl)) {
                    return;
                }

                if (bitmap == null) {
                    applyFallback(imageView, fallbackRes, false);
                    return;
                }

                imageView.setImageBitmap(bitmap);
                imageView.setImageTintList(null);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            });
        });
    }

    private static Bitmap downloadBitmap(String imageUrl) {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);
            connection.connect();

            try (InputStream inputStream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (Exception exception) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void applyFallback(ImageView imageView, @DrawableRes int fallbackRes, boolean tintWhite) {
        imageView.setImageResource(fallbackRes);
        imageView.setImageTintList(tintWhite
                ? imageView.getContext().getColorStateList(android.R.color.white)
                : null);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }
}
