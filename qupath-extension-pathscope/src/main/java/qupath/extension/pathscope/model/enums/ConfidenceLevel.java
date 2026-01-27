package qupath.extension.pathscope.model.enums;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Confidence level assessment
 * 
 * Represents the expert's confidence in their assessment decision.
 * Based on probability ranges:
 * - Level I (1): >75% confidence (Definite)
 * - Level II (2): 50-75% confidence (Suspected)
 * - Level III (3): 25-50% confidence (Inclined)
 * - Level IV (4): <25% confidence (Differential)
 */
public enum ConfidenceLevel {
    LEVEL_I_DEFINITE(1),
    LEVEL_II_SUSPECTED(2),
    LEVEL_III_INCLINED(3),
    LEVEL_IV_DIFFERENTIAL(4);

    private final int value;

    ConfidenceLevel(int value) {
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
        displayNames.put(LEVEL_I_DEFINITE.getValue(), "Level I - Definite (>75%)");
        displayNames.put(LEVEL_II_SUSPECTED.getValue(), "Level II - Suspected (50-75%)");
        displayNames.put(LEVEL_III_INCLINED.getValue(), "Level III - Inclined (25-50%)");
        displayNames.put(LEVEL_IV_DIFFERENTIAL.getValue(), "Level IV - Differential (<25%)");
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
    public static ConfidenceLevel fromDisplayValue(String displayValue) {
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
    public static ConfidenceLevel fromInt(int value) {
        for (ConfidenceLevel level : values()) {
            if (level.getValue() == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    }
}