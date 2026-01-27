package qupath.extension.pathscope.model.enums;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Diagnostic acceptability assessment
 * 
 * Determines whether the image quality is sufficient for making a diagnosis.
 * Uses integer values: 1=Acceptable, 0=Not Acceptable
 */
public enum DiagnosticAcceptability {
    ACCEPTABLE(1),
    NOT_ACCEPTABLE(0);

    private final int value;

    DiagnosticAcceptability(int value) {
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
        displayNames.put(ACCEPTABLE.getValue(), "Acceptable");
        displayNames.put(NOT_ACCEPTABLE.getValue(), "Not Acceptable");
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
    public static DiagnosticAcceptability fromDisplayValue(String displayValue) {
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
    public static DiagnosticAcceptability fromInt(int value) {
        for (DiagnosticAcceptability acceptability : values()) {
            if (acceptability.getValue() == value) {
                return acceptability;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    }
}