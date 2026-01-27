package qupath.extension.pathscope.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import qupath.extension.pathscope.model.enums.ImageQualityGrade;
import qupath.extension.pathscope.model.enums.DiagnosticAcceptability;
import qupath.extension.pathscope.model.enums.TumorExistence;
import qupath.extension.pathscope.model.enums.LesionNature;
import qupath.extension.pathscope.model.enums.ConfidenceLevel;

/**
 * Expert Assessment Data Model
 * 
 * Represents a complete expert pathology assessment including:
 * - Image quality evaluation
 * - Diagnostic acceptability
 * - Tumor presence determination
 * - Lesion nature classification
 * - Confidence levels for assessments
 */
public class ExpertAssessment {
    
    private ImageQualityGrade qualityGrade;
    private DiagnosticAcceptability diagnosticAcceptable;
    private TumorExistence tumorExistence;
    private LesionNature lesionNature;
    private ConfidenceLevel natureConfidence;
    private ConfidenceLevel finalConfidence;
    private String fileId;
    private String expertId;
    private LocalDateTime timestamp;
    private String notes;

    /**
     * Constructor with all required fields
     */
    public ExpertAssessment(
            ImageQualityGrade qualityGrade,
            DiagnosticAcceptability diagnosticAcceptable,
            TumorExistence tumorExistence,
            LesionNature lesionNature,
            ConfidenceLevel natureConfidence,
            ConfidenceLevel finalConfidence,
            String fileId,
            String expertId,
            LocalDateTime timestamp,
            String notes) {
        this.qualityGrade = qualityGrade;
        this.diagnosticAcceptable = diagnosticAcceptable;
        this.tumorExistence = tumorExistence;
        this.lesionNature = lesionNature;
        this.natureConfidence = natureConfidence;
        this.finalConfidence = finalConfidence;
        this.fileId = fileId;
        this.expertId = expertId;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.notes = notes;

        validateEnums();
    }

    /**
     * Constructor with required fields only
     */
    public ExpertAssessment(
            ImageQualityGrade qualityGrade,
            DiagnosticAcceptability diagnosticAcceptable,
            TumorExistence tumorExistence,
            LesionNature lesionNature,
            ConfidenceLevel natureConfidence,
            ConfidenceLevel finalConfidence) {
        this(qualityGrade, diagnosticAcceptable, tumorExistence, lesionNature, 
             natureConfidence, finalConfidence, null, null, null, null);
    }

    /**
     * Validate that all enum fields are of correct type
     */
    private void validateEnums() {
        if (qualityGrade == null) {
            throw new IllegalArgumentException("qualityGrade cannot be null");
        }
        if (diagnosticAcceptable == null) {
            throw new IllegalArgumentException("diagnosticAcceptable cannot be null");
        }
        if (tumorExistence == null) {
            throw new IllegalArgumentException("tumorExistence cannot be null");
        }
        if (lesionNature == null) {
            throw new IllegalArgumentException("lesionNature cannot be null");
        }
        if (natureConfidence == null) {
            throw new IllegalArgumentException("natureConfidence cannot be null");
        }
        if (finalConfidence == null) {
            throw new IllegalArgumentException("finalConfidence cannot be null");
        }
    }

    /**
     * Convert assessment to dictionary format
     * 
     * @return Map with enum values converted to integers
     */
    public Map<String, Object> toDict() {
        Map<String, Object> data = new HashMap<>();
        data.put("quality_grade", qualityGrade.getValue());
        data.put("diagnostic_acceptable", diagnosticAcceptable.getValue());
        data.put("tumor_existence", tumorExistence.getValue());
        data.put("lesion_nature", lesionNature.getValue());
        data.put("nature_confidence", natureConfidence.getValue());
        data.put("final_confidence", finalConfidence.getValue());
        data.put("file_id", fileId);
        data.put("expert_id", expertId);
        data.put("timestamp", timestamp != null ? timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        data.put("notes", notes);
        return data;
    }

    /**
     * Convert assessment to dictionary format with display names
     * 
     * @return Map with enum values as display strings
     */
    public Map<String, Object> toDisplayDict() {
        Map<String, Object> data = new HashMap<>();
        data.put("quality_grade", qualityGrade.getDisplayName());
        data.put("diagnostic_acceptable", diagnosticAcceptable.getDisplayName());
        data.put("tumor_existence", tumorExistence.getDisplayName());
        data.put("lesion_nature", lesionNature.getDisplayName());
        data.put("nature_confidence", natureConfidence.getDisplayName());
        data.put("final_confidence", finalConfidence.getDisplayName());
        data.put("file_id", fileId);
        data.put("expert_id", expertId);
        data.put("timestamp", timestamp != null ? timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        data.put("notes", notes);
        return data;
    }

    /**
     * Create ExpertAssessment instance from dictionary
     * 
     * @param data Dictionary containing assessment data
     * @return New ExpertAssessment instance
     */
    public static ExpertAssessment fromDict(Map<String, Object> data) {
        try {
            ImageQualityGrade qualityGrade = ImageQualityGrade.fromInt((int) data.get("quality_grade"));
            DiagnosticAcceptability diagnosticAcceptable = DiagnosticAcceptability.fromInt((int) data.get("diagnostic_acceptable"));
            TumorExistence tumorExistence = TumorExistence.fromInt((int) data.get("tumor_existence"));
            LesionNature lesionNature = LesionNature.fromInt((int) data.get("lesion_nature"));
            ConfidenceLevel natureConfidence = ConfidenceLevel.fromInt((int) data.get("nature_confidence"));
            ConfidenceLevel finalConfidence = ConfidenceLevel.fromInt((int) data.get("final_confidence"));

            String fileId = (String) data.get("file_id");
            String expertId = (String) data.get("expert_id");
            LocalDateTime timestamp = null;
            if (data.containsKey("timestamp") && data.get("timestamp") != null) {
                timestamp = LocalDateTime.parse((String) data.get("timestamp"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            String notes = (String) data.get("notes");

            return new ExpertAssessment(
                    qualityGrade,
                    diagnosticAcceptable,
                    tumorExistence,
                    lesionNature,
                    natureConfidence,
                    finalConfidence,
                    fileId,
                    expertId,
                    timestamp,
                    notes
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Error creating ExpertAssessment from dict: " + e.getMessage(), e);
        }
    }

    /**
     * Create ExpertAssessment from UI display values
     * 
     * @param qualityGradeDisplay Quality grade display string
     * @param diagnosticAcceptableDisplay Diagnostic acceptability display string
     * @param tumorExistenceDisplay Tumor existence display string
     * @param lesionNatureDisplay Lesion nature display string
     * @param natureConfidenceDisplay Nature confidence display string
     * @param finalConfidenceDisplay Final confidence display string
     * @param fileId Optional file identifier
     * @param expertId Optional expert identifier
     * @param notes Optional notes
     * @return New ExpertAssessment instance
     */
    public static ExpertAssessment fromDisplayValues(
            String qualityGradeDisplay,
            String diagnosticAcceptableDisplay,
            String tumorExistenceDisplay,
            String lesionNatureDisplay,
            String natureConfidenceDisplay,
            String finalConfidenceDisplay,
            String fileId,
            String expertId,
            String notes) {
        return new ExpertAssessment(
                ImageQualityGrade.fromDisplayValue(qualityGradeDisplay),
                DiagnosticAcceptability.fromDisplayValue(diagnosticAcceptableDisplay),
                TumorExistence.fromDisplayValue(tumorExistenceDisplay),
                LesionNature.fromDisplayValue(lesionNatureDisplay),
                ConfidenceLevel.fromDisplayValue(natureConfidenceDisplay),
                ConfidenceLevel.fromDisplayValue(finalConfidenceDisplay),
                fileId,
                expertId,
                null,
                notes
        );
    }

    /**
     * Check if the image quality is high (G1 or G2)
     * 
     * @return True if quality is excellent or good
     */
    public boolean isHighQuality() {
        return qualityGrade == ImageQualityGrade.G1_EXCELLENT || 
               qualityGrade == ImageQualityGrade.G2_GOOD;
    }

    /**
     * Check if the assessment is diagnostically valid
     * 
     * @return True if the image is diagnostically acceptable
     */
    public boolean isDiagnosticallyValid() {
        return diagnosticAcceptable == DiagnosticAcceptability.ACCEPTABLE;
    }

    /**
     * Check if the final diagnosis has high confidence (Level I or II)
     * 
     * @return True if confidence is >50%
     */
    public boolean isHighConfidence() {
        return finalConfidence == ConfidenceLevel.LEVEL_I_DEFINITE || 
               finalConfidence == ConfidenceLevel.LEVEL_II_SUSPECTED;
    }

    /**
     * Get a human-readable summary of the assessment
     * 
     * @return Formatted summary string with display names
     */
    public String getSummary() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(
                "Expert Assessment Summary:\n" +
                "  Quality: %s (value: %d)\n" +
                "  Diagnostic Acceptability: %s (value: %d)\n" +
                "  Tumor Presence: %s (value: %d)\n" +
                "  Lesion Nature: %s (value: %d)\n" +
                "  Nature Confidence: %s (value: %d)\n" +
                "  Final Confidence: %s (value: %d)\n" +
                "  Timestamp: %s",
                qualityGrade.getDisplayName(), qualityGrade.getValue(),
                diagnosticAcceptable.getDisplayName(), diagnosticAcceptable.getValue(),
                tumorExistence.getDisplayName(), tumorExistence.getValue(),
                lesionNature.getDisplayName(), lesionNature.getValue(),
                natureConfidence.getDisplayName(), natureConfidence.getValue(),
                finalConfidence.getDisplayName(), finalConfidence.getValue(),
                timestamp != null ? timestamp.format(formatter) : "N/A"
        );
    }

    @Override
    public String toString() {
        return getSummary();
    }

    // Getters and setters
    public ImageQualityGrade getQualityGrade() {
        return qualityGrade;
    }

    public void setQualityGrade(ImageQualityGrade qualityGrade) {
        this.qualityGrade = qualityGrade;
    }

    public DiagnosticAcceptability getDiagnosticAcceptable() {
        return diagnosticAcceptable;
    }

    public void setDiagnosticAcceptable(DiagnosticAcceptability diagnosticAcceptable) {
        this.diagnosticAcceptable = diagnosticAcceptable;
    }

    public TumorExistence getTumorExistence() {
        return tumorExistence;
    }

    public void setTumorExistence(TumorExistence tumorExistence) {
        this.tumorExistence = tumorExistence;
    }

    public LesionNature getLesionNature() {
        return lesionNature;
    }

    public void setLesionNature(LesionNature lesionNature) {
        this.lesionNature = lesionNature;
    }

    public ConfidenceLevel getNatureConfidence() {
        return natureConfidence;
    }

    public void setNatureConfidence(ConfidenceLevel natureConfidence) {
        this.natureConfidence = natureConfidence;
    }

    public ConfidenceLevel getFinalConfidence() {
        return finalConfidence;
    }

    public void setFinalConfidence(ConfidenceLevel finalConfidence) {
        this.finalConfidence = finalConfidence;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getExpertId() {
        return expertId;
    }

    public void setExpertId(String expertId) {
        this.expertId = expertId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}