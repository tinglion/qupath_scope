package qupath.extension.pathscope.model.enums;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Image quality grading system (G1-G5)
 * 
 * Represents the overall quality of the pathology image for diagnostic purposes.
 * Uses integer values: 1=Excellent, 2=Good, 3=Fair, 4=Poor, 5=Very Poor
 */
public enum ImageQualityGrade {
    G1_EXCELLENT(1),
    G2_GOOD(2),
    G3_FAIR(3),
    G4_POOR(4),
    G5_VERY_POOR(5);

    private final int value;

    ImageQualityGrade(int value) {
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
        displayNames.put(G1_EXCELLENT.getValue(), "G1-Excellent");
        displayNames.put(G2_GOOD.getValue(), "G2-Good");
        displayNames.put(G3_FAIR.getValue(), "G3-Fair");
        displayNames.put(G4_POOR.getValue(), "G4-Poor");
        displayNames.put(G5_VERY_POOR.getValue(), "G5-Very Poor");
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
    public static ImageQualityGrade fromDisplayValue(String displayValue) {
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
    public static ImageQualityGrade fromInt(int value) {
        for (ImageQualityGrade grade : values()) {
            if (grade.getValue() == value) {
                return grade;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    }
}