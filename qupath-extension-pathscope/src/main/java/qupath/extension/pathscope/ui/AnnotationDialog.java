package qupath.extension.pathscope.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import qupath.extension.pathscope.data.TaskFile;
import qupath.extension.pathscope.data.Task;
import qupath.extension.pathscope.data.CacheManager;
import qupath.extension.pathscope.logic.ApiClient;
import qupath.extension.pathscope.model.ExpertAssessment;
import qupath.extension.pathscope.model.enums.ImageQualityGrade;
import qupath.extension.pathscope.model.enums.DiagnosticAcceptability;
import qupath.extension.pathscope.model.enums.TumorExistence;
import qupath.extension.pathscope.model.enums.LesionNature;
import qupath.extension.pathscope.model.enums.ConfidenceLevel;

/**
 * 标注界面，用于显示和编辑WSI文件的标注
 */
public class AnnotationDialog extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationDialog.class);

    private final QuPathGUI qupath;
    private final ApiClient apiClient;
    private final TaskFile taskFile;
    private final CacheManager cacheManager;

    private TextArea annotationTextArea;

    public AnnotationDialog(QuPathGUI qupath, ApiClient apiClient, TaskFile taskFile) {
        this.qupath = qupath;
        this.apiClient = apiClient;
        this.taskFile = taskFile;
        this.cacheManager = new CacheManager();

        initModality(Modality.NONE);
        setTitle("PathScope Annotation");
        setWidth(1000);
        setHeight(700);

        createUI();
        loadAnnotation();
    }

    private ComboBox<String> qualityGradeComboBox;
    private ComboBox<String> diagnosticAcceptableComboBox;
    private ComboBox<String> tumorExistenceComboBox;
    private ComboBox<String> lesionNatureComboBox;
    private ComboBox<String> natureConfidenceComboBox;
    private ComboBox<String> finalConfidenceComboBox;
    private TextField fileIdField;
    private TextField expertIdField;
    private TextArea notesArea;

    private void createUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Create main content area
        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(10));

        // WSI information
        HBox wsiInfoBox = new HBox(10);
        wsiInfoBox.setAlignment(Pos.CENTER_LEFT);
        Label wsiNameLabel = new Label("WSI: ");
        Label wsiNameValue = new Label(taskFile.getWsi() != null ? taskFile.getWsi().getName() : "Unknown");
        wsiNameValue.setStyle("-fx-font-weight: bold;");
        wsiInfoBox.getChildren().addAll(wsiNameLabel, wsiNameValue);
        mainContent.getChildren().add(wsiInfoBox);

        // Annotation text area
        Label annotationLabel = new Label("Annotation:");
        annotationLabel.setStyle("-fx-font-weight: bold;");
        mainContent.getChildren().add(annotationLabel);
        
        annotationTextArea = new TextArea();
        annotationTextArea.setWrapText(true);
        annotationTextArea.setPromptText("Enter annotation here...");
        annotationTextArea.setPrefHeight(200);
        mainContent.getChildren().add(annotationTextArea);

        // Expert Assessment form group
        javafx.scene.control.TitledPane assessmentPane = new javafx.scene.control.TitledPane();
        assessmentPane.setText("Expert Assessment");
        assessmentPane.setCollapsible(false);
        
        GridPane assessmentGrid = new GridPane();
        assessmentGrid.setHgap(10);
        assessmentGrid.setVgap(8);
        assessmentGrid.setPadding(new Insets(10));

        // Quality Grade
        Label qualityGradeLabel = new Label("Image Quality:");
        qualityGradeComboBox = new ComboBox<>();
        qualityGradeComboBox.getItems().addAll(ImageQualityGrade.getDisplayValues());
        qualityGradeComboBox.getSelectionModel().selectFirst();
        assessmentGrid.add(qualityGradeLabel, 0, 0);
        assessmentGrid.add(qualityGradeComboBox, 1, 0);

        // Diagnostic Acceptability
        Label diagnosticAcceptableLabel = new Label("Diagnostic Acceptability:");
        diagnosticAcceptableComboBox = new ComboBox<>();
        diagnosticAcceptableComboBox.getItems().addAll(DiagnosticAcceptability.getDisplayValues());
        diagnosticAcceptableComboBox.getSelectionModel().selectFirst();
        assessmentGrid.add(diagnosticAcceptableLabel, 0, 1);
        assessmentGrid.add(diagnosticAcceptableComboBox, 1, 1);

        // Tumor Existence
        Label tumorExistenceLabel = new Label("Tumor Presence:");
        tumorExistenceComboBox = new ComboBox<>();
        tumorExistenceComboBox.getItems().addAll(TumorExistence.getDisplayValues());
        tumorExistenceComboBox.getSelectionModel().selectFirst();
        assessmentGrid.add(tumorExistenceLabel, 0, 2);
        assessmentGrid.add(tumorExistenceComboBox, 1, 2);

        // Lesion Nature
        Label lesionNatureLabel = new Label("Lesion Nature:");
        lesionNatureComboBox = new ComboBox<>();
        lesionNatureComboBox.getItems().addAll(LesionNature.getDisplayValues());
        lesionNatureComboBox.getSelectionModel().selectFirst();
        assessmentGrid.add(lesionNatureLabel, 0, 3);
        assessmentGrid.add(lesionNatureComboBox, 1, 3);

        // Nature Confidence
        Label natureConfidenceLabel = new Label("Nature Confidence:");
        natureConfidenceComboBox = new ComboBox<>();
        natureConfidenceComboBox.getItems().addAll(ConfidenceLevel.getDisplayValues());
        natureConfidenceComboBox.getSelectionModel().selectFirst();
        assessmentGrid.add(natureConfidenceLabel, 0, 4);
        assessmentGrid.add(natureConfidenceComboBox, 1, 4);

        // Final Confidence
        Label finalConfidenceLabel = new Label("Final Confidence:");
        finalConfidenceComboBox = new ComboBox<>();
        finalConfidenceComboBox.getItems().addAll(ConfidenceLevel.getDisplayValues());
        finalConfidenceComboBox.getSelectionModel().selectFirst();
        assessmentGrid.add(finalConfidenceLabel, 0, 5);
        assessmentGrid.add(finalConfidenceComboBox, 1, 5);

        // File ID and Expert ID fields are hidden but still available for programmatic use
        fileIdField = new TextField();
        fileIdField.setVisible(false);
        expertIdField = new TextField();
        expertIdField.setVisible(false);

        // Notes
        Label notesLabel = new Label("Notes:");
        notesArea = new TextArea();
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(3);
        assessmentGrid.add(notesLabel, 0, 8);
        assessmentGrid.add(notesArea, 1, 8);

        assessmentPane.setContent(assessmentGrid);
        mainContent.getChildren().add(assessmentPane);

        // Create buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button saveButton = new Button("Save Annotation");
        saveButton.setOnAction(e -> saveAnnotation());
        saveButton.setPrefWidth(150);

        Button submitButton = new Button("Submit Annotation");
        submitButton.setOnAction(e -> submitAnnotation());
        submitButton.setPrefWidth(150);
        submitButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> close());
        cancelButton.setPrefWidth(100);

        buttonBox.getChildren().addAll(saveButton, submitButton, cancelButton);
        mainContent.getChildren().add(buttonBox);

        root.setCenter(mainContent);

        Scene scene = new Scene(root);
        setScene(scene);
    }

    private void loadAnnotation() {
        // Load existing annotation if available
        Map<String, Object> annotation = taskFile.getAnnotation();
        if (annotation != null && !annotation.isEmpty()) {
            // Convert annotation map to string for display
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : annotation.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            annotationTextArea.setText(sb.toString());
        }
    }

    private void saveAnnotation() {
        // Save annotation to task file
        String annotationText = annotationTextArea.getText();
        Map<String, Object> annotation = new HashMap<>();
        
        // Parse annotation text into map (simple key-value pairs)
        // This is optional - if no annotation text, just use an empty map
        if (annotationText != null && !annotationText.trim().isEmpty()) {
            String[] lines = annotationText.split("\n");
            for (String line : lines) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    annotation.put(key, value);
                }
            }
        }
        
        // Add expert assessment data
        ExpertAssessment assessment = createExpertAssessment();
        Map<String, Object> assessmentData = assessment.toDict();
        annotation.putAll(assessmentData);
        
        taskFile.setAnnotation(annotation);
        taskFile.setAnnotated(true);
        taskFile.setLocalStatus("annotated");
        
        // Save task to cache
        try {
            Task task = taskFile.getTask();
            if (task != null) {
                cacheManager.saveTask(task);
            } else {
                logger.warn("Task is null, skipping cache save");
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("Annotation saved successfully");
            alert.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to save annotation: {}", e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to save annotation");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Create ExpertAssessment from form data
     */
    private ExpertAssessment createExpertAssessment() {
        return ExpertAssessment.fromDisplayValues(
                qualityGradeComboBox.getValue(),
                diagnosticAcceptableComboBox.getValue(),
                tumorExistenceComboBox.getValue(),
                lesionNatureComboBox.getValue(),
                natureConfidenceComboBox.getValue(),
                finalConfidenceComboBox.getValue(),
                fileIdField.getText(),
                expertIdField.getText(),
                notesArea.getText()
        );
    }

    private void submitAnnotation() {
        // First save annotation
        saveAnnotation();
        
        // Then submit to server
        new Thread(() -> {
            try {
                // Get WSI ID from task file or WSI object
                int wsiId = 0;
                if (taskFile.getId() != null) {
                    try {
                        wsiId = Integer.parseInt(taskFile.getId());
                    } catch (NumberFormatException e) {
                        // Try to get ID from WSI object
                        if (taskFile.getWsi() != null) {
                            wsiId = taskFile.getWsi().getId();
                        }
                    }
                } else if (taskFile.getWsi() != null) {
                    wsiId = taskFile.getWsi().getId();
                }
                
                if (wsiId > 0) {
                    boolean success = apiClient.submitAnnotation(wsiId, taskFile.getAnnotation());
                    Platform.runLater(() -> {
                        if (success) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Success");
                            alert.setHeaderText("Annotation submitted successfully");
                            alert.showAndWait();
                            close();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Failed to submit annotation");
                            alert.showAndWait();
                        }
                    });
                } else {
                    throw new IOException("Invalid WSI ID");
                }
            } catch (IOException e) {
                logger.error("Failed to submit annotation: {}", e.getMessage());
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to submit annotation");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }
}
