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
}
