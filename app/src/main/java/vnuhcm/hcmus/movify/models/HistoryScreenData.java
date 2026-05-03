package vnuhcm.hcmus.movify.models;

import java.util.List;

public class HistoryScreenData {
    private final List<History> items;
    private final String totalAmount;
    private final String totalTrips;
    private final String totalDistance;
    private final String periodLabel;

    public HistoryScreenData(List<History> items,
                             String totalAmount,
                             String totalTrips,
                             String totalDistance,
                             String periodLabel) {
        this.items = items;
        this.totalAmount = totalAmount;
        this.totalTrips = totalTrips;
        this.totalDistance = totalDistance;
        this.periodLabel = periodLabel;
    }

    public List<History> getItems() {
        return items;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public String getTotalTrips() {
        return totalTrips;
    }

    public String getTotalDistance() {
        return totalDistance;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }
}
