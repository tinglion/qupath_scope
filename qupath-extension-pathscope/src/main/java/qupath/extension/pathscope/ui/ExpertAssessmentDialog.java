package qupath.extension.pathscope.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import qupath.lib.gui.QuPathGUI;
import qupath.extension.pathscope.model.ExpertAssessment;
import qupath.extension.pathscope.model.enums.ImageQualityGrade;
import qupath.extension.pathscope.model.enums.DiagnosticAcceptability;
import qupath.extension.pathscope.model.enums.TumorExistence;
import qupath.extension.pathscope.model.enums.LesionNature;
import qupath.extension.pathscope.model.enums.ConfidenceLevel;

import java.util.Optional;

/**
 * Expert Assessment Dialog
 * 
 * Provides a user interface for pathologists to enter expert assessments
 * including image quality, diagnostic acceptability, tumor presence, 
 * lesion nature, and confidence levels.
 */
public class ExpertAssessmentDialog extends Dialog<ExpertAssessment> {

    private ComboBox<String> qualityGradeComboBox;
    private ComboBox<String> diagnosticAcceptableComboBox;
    private ComboBox<String> tumorExistenceComboBox;
    private ComboBox<String> lesionNatureComboBox;
    private ComboBox<String> natureConfidenceComboBox;
    private ComboBox<String> finalConfidenceComboBox;
    private TextField fileIdField;
    private TextField expertIdField;
    private TextArea notesArea;

    /**
     * Constructor
     * 
     * @param qupath QuPathGUI instance
     * @param fileId Optional file ID to pre-fill
     */
    public ExpertAssessmentDialog(QuPathGUI qupath, String fileId) {
        setTitle("Expert Assessment");
        setHeaderText("Please complete the expert assessment form");
        initModality(Modality.APPLICATION_MODAL);

        // Set dialog owner
        Stage stage = (Stage) qupath.getStage();
        if (stage != null) {
            initOwner(stage);
        }

        // Create UI components
        createUI();

        // Pre-fill file ID if provided
        if (fileId != null && !fileId.isEmpty()) {
            fileIdField.setText(fileId);
        }

        // Set up buttons
        setupButtons();

        // Set result converter
        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return createAssessment();
            }
            return null;
        });
    }

    /**
     * Create UI components
     */
    private void createUI() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        // Create form grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(5, 10, 5, 10));

        // Quality Grade
        Label qualityGradeLabel = new Label("Image Quality:");
        qualityGradeComboBox = new ComboBox<>();
        qualityGradeComboBox.getItems().addAll(ImageQualityGrade.getDisplayValues());
        qualityGradeComboBox.getSelectionModel().selectFirst();
        grid.add(qualityGradeLabel, 0, 0);
        grid.add(qualityGradeComboBox, 1, 0);

        // Diagnostic Acceptability
        Label diagnosticAcceptableLabel = new Label("Diagnostic Acceptability:");
        diagnosticAcceptableComboBox = new ComboBox<>();
        diagnosticAcceptableComboBox.getItems().addAll(DiagnosticAcceptability.getDisplayValues());
        diagnosticAcceptableComboBox.getSelectionModel().selectFirst();
        grid.add(diagnosticAcceptableLabel, 0, 1);
        grid.add(diagnosticAcceptableComboBox, 1, 1);

        // Tumor Existence
        Label tumorExistenceLabel = new Label("Tumor Presence:");
        tumorExistenceComboBox = new ComboBox<>();
        tumorExistenceComboBox.getItems().addAll(TumorExistence.getDisplayValues());
        tumorExistenceComboBox.getSelectionModel().selectFirst();
        grid.add(tumorExistenceLabel, 0, 2);
        grid.add(tumorExistenceComboBox, 1, 2);

        // Lesion Nature
        Label lesionNatureLabel = new Label("Lesion Nature:");
        lesionNatureComboBox = new ComboBox<>();
        lesionNatureComboBox.getItems().addAll(LesionNature.getDisplayValues());
        lesionNatureComboBox.getSelectionModel().selectFirst();
        grid.add(lesionNatureLabel, 0, 3);
        grid.add(lesionNatureComboBox, 1, 3);

        // Nature Confidence
        Label natureConfidenceLabel = new Label("Nature Confidence:");
        natureConfidenceComboBox = new ComboBox<>();
        natureConfidenceComboBox.getItems().addAll(ConfidenceLevel.getDisplayValues());
        natureConfidenceComboBox.getSelectionModel().selectFirst();
        grid.add(natureConfidenceLabel, 0, 4);
        grid.add(natureConfidenceComboBox, 1, 4);

        // Final Confidence
        Label finalConfidenceLabel = new Label("Final Confidence:");
        finalConfidenceComboBox = new ComboBox<>();
        finalConfidenceComboBox.getItems().addAll(ConfidenceLevel.getDisplayValues());
        finalConfidenceComboBox.getSelectionModel().selectFirst();
        grid.add(finalConfidenceLabel, 0, 5);
        grid.add(finalConfidenceComboBox, 1, 5);

        // File ID
        Label fileIdLabel = new Label("File ID:");
        fileIdField = new TextField();
        grid.add(fileIdLabel, 0, 6);
        grid.add(fileIdField, 1, 6);

        // Expert ID
        Label expertIdLabel = new Label("Expert ID:");
        expertIdField = new TextField();
        grid.add(expertIdLabel, 0, 7);
        grid.add(expertIdField, 1, 7);

        // Notes
        Label notesLabel = new Label("Notes:");
        notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        grid.add(notesLabel, 0, 8);
        grid.add(notesArea, 1, 8);

        vbox.getChildren().add(grid);
        getDialogPane().setContent(vbox);
    }

    /**
     * Set up dialog buttons
     */
    private void setupButtons() {
        ButtonType okButton = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButton, cancelButton);

        // Enable/disable OK button based on form validity
        Platform.runLater(() -> {
            Button okBtn = (Button) getDialogPane().lookupButton(okButton);
            if (okBtn != null) {
                okBtn.setDisable(false); // All fields have default values
            }
        });
    }

    /**
     * Create ExpertAssessment from form data
     * 
     * @return ExpertAssessment instance
     */
    private ExpertAssessment createAssessment() {
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


}