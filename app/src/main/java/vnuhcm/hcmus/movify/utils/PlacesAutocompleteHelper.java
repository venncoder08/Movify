package vnuhcm.hcmus.movify.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.services.PlaceService;

public class PlacesAutocompleteHelper {
    private static final Filter NO_OP_FILTER = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            results.values = null;
            results.count = 0;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            // Intentionally no-op. The helper manages suggestion lists itself.
        }
    };

    public interface SpecialSuggestionListener {
        void onCurrentLocationSelected(AutoCompleteTextView targetView);
    }

    private final String currentLocationLabel;
    private final AutoCompleteTextView autoCompleteTextView;
    private final String historyKey;
    private ArrayAdapter<String> adapter;
    private List<String> predictions = new ArrayList<>();
    private Context context;
    private SpecialSuggestionListener specialSuggestionListener;
    private boolean isSettingText = false; // Flag to prevent TextWatcher interference
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingFetch;
    private static final int DEBOUNCE_DELAY_MS = 250;
    private static final int MIN_REMOTE_QUERY_LENGTH = 2;
    private static final int MAX_CACHE_ENTRIES = 30;
    private final Map<String, List<String>> cache = new LinkedHashMap<String, List<String>>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    public PlacesAutocompleteHelper(Context context, AutoCompleteTextView autoCompleteTextView) {
        this(context, autoCompleteTextView, null, null);
    }

    public PlacesAutocompleteHelper(Context context,
                                    AutoCompleteTextView autoCompleteTextView,
                                    SpecialSuggestionListener specialSuggestionListener) {
        this(context, autoCompleteTextView, specialSuggestionListener, null);
    }

    public PlacesAutocompleteHelper(Context context,
                                    AutoCompleteTextView autoCompleteTextView,
                                    SpecialSuggestionListener specialSuggestionListener,
                                    String historyKey) {
        this.context = context;
        this.autoCompleteTextView = autoCompleteTextView;
        this.specialSuggestionListener = specialSuggestionListener;
        this.historyKey = historyKey;
        this.currentLocationLabel = context.getString(R.string.current_location);

        // Setup ArrayAdapter for suggestions
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, predictions) {
            @Override
            public Filter getFilter() {
                return NO_OP_FILTER;
            }
        };
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setDropDownHeight(400);
        autoCompleteTextView.setThreshold(0);
        autoCompleteTextView.setOnClickListener(v -> showLocalPredictions(autoCompleteTextView.getText().toString()));
        autoCompleteTextView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showLocalPredictions(autoCompleteTextView.getText().toString());
            }
        });

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

                String query = s == null ? "" : s.toString().trim();
                if (!isSettingText && query.length() >= MIN_REMOTE_QUERY_LENGTH) {
                    pendingFetch = () -> fetchPredictions(query);
                    handler.postDelayed(pendingFetch, DEBOUNCE_DELAY_MS);
                } else if (!isSettingText) {
                    showLocalPredictions(query);
                } else if (s.length() == 0) {
                    showLocalPredictions("");
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void fetchPredictions(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < MIN_REMOTE_QUERY_LENGTH) {
            showLocalPredictions(normalizedQuery);
            return;
        }

        // Check cache first
        if (cache.containsKey(normalizedQuery)) {
            updatePredictions(normalizedQuery, cache.get(normalizedQuery));
            return;
        }

        for (Map.Entry<String, List<String>> entry : cache.entrySet()) {
            if (normalizedQuery.startsWith(entry.getKey())) {
                List<String> cachedSuggestions = filterSuggestions(normalizedQuery, entry.getValue());
                if (!cachedSuggestions.isEmpty()) {
                    updatePredictions(normalizedQuery, cachedSuggestions);
                }
                break;
            }
        }

        if (!normalizedQuery.equals(autoCompleteTextView.getText().toString().trim())) {
            return;
        }

        // Fetch on background thread
        new Thread(() -> {
            List<String> results = new ArrayList<>();

            try {
                results = PlaceService.fetchAutocompleteSuggestions(context, normalizedQuery);
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            List<String> finalResults = results;

            // Cache and update UI on main thread
            cache.put(normalizedQuery, finalResults);
            handler.post(() -> updatePredictions(normalizedQuery, finalResults));
        }).start();
    }

    private void showLocalPredictions(String query) {
        updatePredictions(query, new ArrayList<>());
    }

    private void updatePredictions(String query, List<String> results) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (!TextUtils.isEmpty(normalizedQuery)
                && !normalizedQuery.equals(autoCompleteTextView.getText().toString().trim())) {
            return;
        }

        List<String> mergedPredictions = new ArrayList<>();
        addDistinct(mergedPredictions, currentLocationLabel);

        for (String historyItem : getFilteredHistory(query)) {
            addDistinct(mergedPredictions, historyItem);
        }

        for (String result : results) {
            addDistinct(mergedPredictions, result);
        }

        predictions.clear();
        predictions.addAll(mergedPredictions);

        adapter.clear();
        adapter.addAll(mergedPredictions);
        adapter.notifyDataSetChanged();

        if (autoCompleteTextView.hasFocus() && !mergedPredictions.isEmpty()) {
            autoCompleteTextView.showDropDown();
        } else if (mergedPredictions.isEmpty()) {
            autoCompleteTextView.dismissDropDown();
        }
    }

    private List<String> getFilteredHistory(String query) {
        List<String> filteredHistory = new ArrayList<>();
        if (TextUtils.isEmpty(historyKey)) {
            return filteredHistory;
        }

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        for (String historyItem : LocationHistoryStore.getHistory(context, historyKey)) {
            if (TextUtils.isEmpty(normalizedQuery)
                    || historyItem.toLowerCase().contains(normalizedQuery)) {
                filteredHistory.add(historyItem);
            }
        }

        return filteredHistory;
    }

    private void addDistinct(List<String> target, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }

        for (String existing : target) {
            if (existing.equalsIgnoreCase(value)) {
                return;
            }
        }

        target.add(value);
    }

    private List<String> filterSuggestions(String query, List<String> sourceSuggestions) {
        List<String> filtered = new ArrayList<>();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        for (String suggestion : sourceSuggestions) {
            if (TextUtils.isEmpty(normalizedQuery)
                    || suggestion.toLowerCase().contains(normalizedQuery)) {
                addDistinct(filtered, suggestion);
            }
        }
        return filtered;
    }

    public String getSelectedPlaceId(int position) {
        if (position < predictions.size()) {
            return predictions.get(position);
        }
        return null;
    }
}
