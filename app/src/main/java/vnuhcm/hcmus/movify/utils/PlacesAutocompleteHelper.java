package vnuhcm.hcmus.movify.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.services.PlaceService;

public class PlacesAutocompleteHelper {
    public interface SpecialSuggestionListener {
        void onCurrentLocationSelected(AutoCompleteTextView targetView);
    }

    private final String currentLocationLabel;
    private ArrayAdapter<String> adapter;
    private List<String> predictions = new ArrayList<>();
    private Context context;
    private SpecialSuggestionListener specialSuggestionListener;
    private boolean isSettingText = false; // Flag to prevent TextWatcher interference
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingFetch;
    private static final int DEBOUNCE_DELAY = 500; // 500ms debounce
    private Map<String, List<String>> cache = new HashMap<>(); // Cache results

    public PlacesAutocompleteHelper(Context context, AutoCompleteTextView autoCompleteTextView) {
        this(context, autoCompleteTextView, null);
    }

    public PlacesAutocompleteHelper(Context context,
                                    AutoCompleteTextView autoCompleteTextView,
                                    SpecialSuggestionListener specialSuggestionListener) {
        this.context = context;
        this.specialSuggestionListener = specialSuggestionListener;
        this.currentLocationLabel = context.getString(R.string.current_location);

        // Setup ArrayAdapter for suggestions
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, predictions);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setDropDownHeight(400);
        autoCompleteTextView.setThreshold(1); // Show suggestions after 1 character

        // Setup autocomplete listener
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < predictions.size()) {
                String prediction = predictions.get(position);
                if (currentLocationLabel.equals(prediction) && specialSuggestionListener != null) {
                    specialSuggestionListener.onCurrentLocationSelected(autoCompleteTextView);
                    autoCompleteTextView.dismissDropDown();
                    return;
                }

                isSettingText = true;
                autoCompleteTextView.setText(prediction);
                isSettingText = false;
                autoCompleteTextView.dismissDropDown();
            }
        });

        // Text change listener for autocomplete with debouncing
        autoCompleteTextView.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous pending fetch
                if (pendingFetch != null) {
                    handler.removeCallbacks(pendingFetch);
                }

                if (!isSettingText && s.length() >= 1) {
                    // Debounce: wait 500ms before fetching
                    pendingFetch = () -> fetchPredictions(s.toString());
                    handler.postDelayed(pendingFetch, DEBOUNCE_DELAY);
                } else if (s.length() == 0) {
                    adapter.clear();
                    predictions.clear();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void fetchPredictions(String query) {
        // Check cache first
        if (cache.containsKey(query)) {
            updatePredictions(cache.get(query));
            return;
        }

        // Fetch on background thread
        new Thread(() -> {
            List<String> results = new ArrayList<>();

            try {
                results = PlaceService.fetchAutocompleteSuggestions(context, query);
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            List<String> finalResults = results;

            // Cache and update UI on main thread
            cache.put(query, finalResults);
            handler.post(() -> updatePredictions(finalResults));
        }).start();
    }

    private void updatePredictions(List<String> results) {
        predictions.clear();
        predictions.add(currentLocationLabel);
        predictions.addAll(results);
        adapter.clear();
        adapter.add(currentLocationLabel);
        adapter.addAll(results);
        adapter.notifyDataSetChanged();
    }

    public String getSelectedPlaceId(int position) {
        if (position < predictions.size()) {
            return predictions.get(position);
        }
        return null;
    }
}
