package qupath.extension.pathscope.model.enums;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Tumor existence assessment
 * 
 * Indicates whether tumor tissue is present in the sample.
 * Uses integer values: 1=Yes, 0=No, 2=Uncertain
 */
public enum TumorExistence {
    YES(1),
    NO(0),
    UNCERTAIN(2);

    private final int value;

    TumorExistence(int value) {
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
        displayNames.put(YES.getValue(), "Yes");
        displayNames.put(NO.getValue(), "No");
        displayNames.put(UNCERTAIN.getValue(), "Uncertain");
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
    public static TumorExistence fromDisplayValue(String displayValue) {
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
    public static TumorExistence fromInt(int value) {
        for (TumorExistence existence : values()) {
            if (existence.getValue() == value) {
                return existence;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    }
}