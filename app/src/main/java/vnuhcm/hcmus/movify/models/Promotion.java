package vnuhcm.hcmus.movify.models;

public class Promotion {
    private int id;
    private String code;
    private String title;
    private String description;
    private String type;
    private double discountValue;
    private boolean selected;

    public Promotion() {
        this(0, "", "", "", 0d);
    }

    public Promotion(int id, String title, String description, String type, double discountValue) {
        this.id = id;
        this.code = title;
        this.title = title;
        this.description = description;
        this.type = type;
        this.discountValue = discountValue;
        this.selected = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        this.title = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
