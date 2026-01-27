package qupath.extension.pathscope.model.enums;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Lesion nature classification
 * 
 * Classifies the nature of the observed lesion.
 * Uses integer values: 0=Benign, 1=Borderline, 2=Malignant, 3=Indeterminate
 */
public enum LesionNature {
    BENIGN(0),
    BORDERLINE(1),
    MALIGNANT(2),
    INDETERMINATE(3);

    private final int value;

    LesionNature(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Get mapping of integer values to display names
     */
    public static Map<Integer, String> getDisplayNames() {
        Map<Integer, String> displayNames = new HashMap<>();
        displayNames.put(BENIGN.getValue(), "Benign");
        displayNames.put(BORDERLINE.getValue(), "Borderline");
        displayNames.put(MALIGNANT.getValue(), "Malignant");
        displayNames.put(INDETERMINATE.getValue(), "Indeterminate");
        return displayNames;
    }

    /**
     * Get display name for this enum value
     */
    public String getDisplayName() {
        return getDisplayNames().get(this.value);
    }

    /**
     * Get list of display values for UI components
     */
    public static List<String> getDisplayValues() {
        return new ArrayList<>(getDisplayNames().values());
    }

    /**
     * Get enum member from display value
     */
    public static LesionNature fromDisplayValue(String displayValue) {
        Map<Integer, String> displayMap = getDisplayNames();
        for (Map.Entry<Integer, String> entry : displayMap.entrySet()) {
            if (entry.getValue().equals(displayValue)) {
                return fromInt(entry.getKey());
            }
        }
        throw new IllegalArgumentException("Invalid display value: " + displayValue);
    }

    /**
     * Get enum member from integer value
     */
    public static LesionNature fromInt(int value) {
        for (LesionNature nature : values()) {
            if (nature.getValue() == value) {
                return nature;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    }
}