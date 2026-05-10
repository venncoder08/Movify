package vnuhcm.hcmus.movify.services;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import vnuhcm.hcmus.movify.models.Promotion;

public final class PromotionService {

    private PromotionService() {
    }

    @NonNull
    public static List<Promotion> fetchPromotions(Context context) throws IOException, JSONException {
        String responseBody = BackendApiService.executeGet(
                context,
                "api/promotions",
                new LinkedHashMap<>()
        );

        JSONObject root = new JSONObject(responseBody);
        JSONArray promotionsJson = root.optJSONArray("data");
        List<Promotion> promotions = new ArrayList<>();

        if (promotionsJson == null) {
            return promotions;
        }

        for (int index = 0; index < promotionsJson.length(); index++) {
            JSONObject promotionJson = promotionsJson.optJSONObject(index);
            if (promotionJson == null) {
                continue;
            }

            promotions.add(new Promotion(
                    promotionJson.optInt("id", 0),
                    promotionJson.optString("title", ""),
                    promotionJson.optString("description", ""),
                    promotionJson.optString("type", ""),
                    promotionJson.optDouble("discountValue", 0d)
            ));
        }

        return promotions;
    }

    public static Promotion createPromotion(Context context, Promotion promotion) throws IOException, JSONException {
        org.json.JSONObject requestBody = new org.json.JSONObject();
        requestBody.put("name", promotion.getCode());
        requestBody.put("code", promotion.getCode());
        requestBody.put("type", promotion.getType());
        requestBody.put("discountValue", promotion.getDiscountValue());

        String jwtToken = new AuthApiService(context).getJwtToken();
        String responseBody = BackendApiService.executeAuthenticatedPost(context, "api/promotions", jwtToken, requestBody);
        org.json.JSONObject root = new org.json.JSONObject(responseBody);
        org.json.JSONObject data = root.optJSONObject("data");
        if (data == null) return null;

        return new Promotion(
                data.optInt("id", 0),
                data.optString("title", data.optString("code", "")),
                data.optString("description", ""),
                data.optString("type", ""),
                data.optDouble("discountValue", 0d)
        );
    }

    public static Promotion updatePromotion(Context context, Promotion promotion) throws IOException, JSONException {
        org.json.JSONObject requestBody = new org.json.JSONObject();
        requestBody.put("name", promotion.getCode());
        requestBody.put("code", promotion.getCode());
        requestBody.put("type", promotion.getType());
        requestBody.put("discountValue", promotion.getDiscountValue());

        String jwtToken = new AuthApiService(context).getJwtToken();
        String path = String.format("api/promotions/%d", promotion.getId());
        String responseBody = BackendApiService.executeAuthenticatedPut(context, path, jwtToken, requestBody);
        org.json.JSONObject root = new org.json.JSONObject(responseBody);
        org.json.JSONObject data = root.optJSONObject("data");
        if (data == null) return null;

        return new Promotion(
                data.optInt("id", 0),
                data.optString("title", data.optString("code", "")),
                data.optString("description", ""),
                data.optString("type", ""),
                data.optDouble("discountValue", 0d)
        );
    }

    public static boolean deletePromotion(Context context, int promotionId) throws IOException {
        String jwtToken = new AuthApiService(context).getJwtToken();
        String path = String.format("api/promotions/%d", promotionId);
        BackendApiService.executeAuthenticatedDelete(context, path, jwtToken);
        return true;
    }
}
