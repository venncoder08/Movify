package vnuhcm.hcmus.movify.models;

public class Promotion {
    private final String title;
    private final String description;
    private boolean selected;

    public Promotion(String title, String description) {
        this.title = title;
        this.description = description;
        this.selected = false;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}