package vnuhcm.hcmus.movify.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import vnuhcm.hcmus.movify.R;

public final class RatingService {

    private RatingService() {
    }

    @NonNull
    public static String rateDriver(@NonNull Context context,
                                    @NonNull String tripId,
                                    float score,
                                    @Nullable String comment)
            throws IOException, JSONException {
        String jwtToken = new AuthApiService(context).getJwtToken();
        if (jwtToken == null || jwtToken.isBlank()) {
            throw new IOException(context.getString(R.string.missing_login_session));
        }

        JSONObject body = new JSONObject();
        body.put("score", Math.max(1f, Math.min(5f, score)));
        if (comment != null && !comment.trim().isEmpty()) {
            body.put("comment", comment.trim());
        }

        String responseBody = BackendApiService.executeAuthenticatedPost(
                context,
                "api/trips/" + tripId + "/rating",
                jwtToken,
                body
        );

        JSONObject root = new JSONObject(responseBody);
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            return context.getString(R.string.rating_thanks);
        }

        return data.optString("message", context.getString(R.string.rating_thanks));
    }
}
