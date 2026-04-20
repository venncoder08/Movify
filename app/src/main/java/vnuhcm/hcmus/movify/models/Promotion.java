package vnuhcm.hcmus.movify.models;

public class Promotion {
    private final int id;
    private final String title;
    private final String description;
    private final String type;
    private final double discountValue;
    private boolean selected;

    public Promotion(String title, String description) {
        this(0, title, description, "", 0d);
    }

    public Promotion(int id, String title, String description, String type, double discountValue) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.discountValue = discountValue;
        this.selected = false;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
