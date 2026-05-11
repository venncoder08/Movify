package vnuhcm.hcmus.movify.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
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

    public static void loadCircularAvatar(ImageView imageView, @Nullable String imageUrl) {
        imageView.setTag(imageUrl);

        if (TextUtils.isEmpty(imageUrl)) {
            applyCircularAvatarFallback(imageView);
            return;
        }

        EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(imageUrl);
            Bitmap circularBitmap = bitmap == null ? null : cropCircle(bitmap);
            MAIN_HANDLER.post(() -> {
                Object currentTag = imageView.getTag();
                if (currentTag == null || !currentTag.equals(imageUrl)) {
                    return;
                }

                if (circularBitmap == null) {
                    applyCircularAvatarFallback(imageView);
                    return;
                }

                imageView.setPadding(0, 0, 0, 0);
                imageView.setImageBitmap(circularBitmap);
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

    private static Bitmap cropCircle(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int left = (source.getWidth() - size) / 2;
        int top = (source.getHeight() - size) / 2;
        Bitmap squared = Bitmap.createBitmap(source, left, top, size, size);
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);

        return output;
    }

    private static void applyCircularAvatarFallback(ImageView imageView) {
        int size = resolveViewSize(imageView);

        int defaultPadding = Math.round(12f * imageView.getResources().getDisplayMetrics().density);
        int padding = size > 0 ? Math.max(defaultPadding, size / 4) : defaultPadding;
        imageView.setPadding(padding, padding, padding, padding);
        applyFallback(imageView, R.drawable.ic_user, true);
    }

    private static int resolveViewSize(ImageView imageView) {
        int width = imageView.getWidth();
        int height = imageView.getHeight();
        if (width > 0 && height > 0) {
            return Math.min(width, height);
        }

        if (imageView.getLayoutParams() != null) {
            width = imageView.getLayoutParams().width;
            height = imageView.getLayoutParams().height;
            if (width > 0 && height > 0) {
                return Math.min(width, height);
            }
        }

        if (imageView.getParent() instanceof View) {
            View parent = (View) imageView.getParent();
            if (parent.getLayoutParams() != null) {
                width = parent.getLayoutParams().width;
                height = parent.getLayoutParams().height;
                if (width > 0 && height > 0) {
                    return Math.min(width, height);
                }
            }
        }

        return 0;
    }

    private static void applyFallback(ImageView imageView, @DrawableRes int fallbackRes, boolean tintWhite) {
        imageView.setImageResource(fallbackRes);
        imageView.setImageTintList(tintWhite
                ? imageView.getContext().getColorStateList(android.R.color.white)
                : null);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }
}
