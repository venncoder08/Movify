const CURRENT_LOCATION_PREFIX = "Vị trí hiện tại";
const MAP_SELECTED_LOCATION_LABEL = "Vị trí đã chọn trên bản đồ";
const TRAILING_COORDINATES_PATTERN = /\s*\(-?\d{1,2}\.\d{3,}\s*,\s*-?\d{1,3}\.\d{3,}\)\s*$/;

function stripCurrentLocationLabel(value) {
  if (typeof value !== "string") {
    return value;
  }

  let label = value.trim().replace(TRAILING_COORDINATES_PATTERN, "").trim();
  if (label.toLowerCase().startsWith(CURRENT_LOCATION_PREFIX.toLowerCase())) {
    label = label.slice(CURRENT_LOCATION_PREFIX.length).trim();
    if (label.startsWith("-") || label.startsWith(":")) {
      label = label.slice(1).trim();
    }
  }

  return label || MAP_SELECTED_LOCATION_LABEL;
}

module.exports = {
  stripCurrentLocationLabel
};
