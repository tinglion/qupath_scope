package qupath.extension.pathscope.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import qupath.lib.gui.QuPathGUI;

import java.util.Optional;

import qupath.extension.pathscope.logic.ApiClient;

/**
 * Login dialog for API authentication.
 */
public class LoginDialog {

    private final QuPathGUI qupath;
    private final ApiClient apiClient;

    public LoginDialog(QuPathGUI qupath, ApiClient apiClient) {
        this.qupath = qupath;
        this.apiClient = apiClient;
    }

    /**
     * Show the login dialog.
     * 
     * @return true if login was successful, false otherwise
     */
    public boolean showAndWait() {
        // Create the dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("API Login");
        dialog.setHeaderText("Please enter your credentials to connect to the API");

        // Set the button types
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, cancelButtonType);

        // Create the username and password labels and fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default
        Platform.runLater(() -> username.requestFocus());

        // Show the dialog and wait for response
        Optional<ButtonType> result = dialog.showAndWait();

        // Check if login was clicked and perform login
        if (result.isPresent() && result.get() == loginButtonType) {
            return login(username.getText(), password.getText());
        }
        return false;
    }

    private boolean login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            showError("Username cannot be empty");
            return false;
        }
        if (password == null || password.trim().isEmpty()) {
            showError("Password cannot be empty");
            return false;
        }

        try {
            boolean success = apiClient.login(username, password);
            if (success) {
                showInfo("Login successful!");
                return true;
            } else {
                showError("Login failed. Please check your credentials.");
                return false;
            }
        } catch (Exception e) {
            showError("Login failed: " + e.getMessage());
            return false;
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
