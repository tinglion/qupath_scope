package qupath.extension.pathscope.ui;

import javafx.scene.control.Separator;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.actions.ActionTools;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.extension.pathscope.logic.ApiIntegrationCommands;

/**
 * Extension for integrating with external API for task management and annotation submission.
 */
public class ApiIntegrationExtension implements QuPathExtension {

    @Override
    public void installExtension(QuPathGUI qupath) {
        // Install PathScope commands using annotation system
        var pathScopeCommands = new ApiIntegrationCommands.PathScopeCommands(qupath);
        qupath.installActions(ActionTools.getAnnotatedActions(pathScopeCommands));
        
        // Add PathScope button to toolbar
        var toolbar = qupath.getToolBar();
        if (toolbar != null) {
            // Create button with text for PathScope main functionality
            // Using createButton instead of createButtonWithGraphicOnly to show text
            var pathScopeButton = ActionTools.createButton(pathScopeCommands.actionPathScope);
            
            // Add separator if toolbar is not empty
            if (!toolbar.getItems().isEmpty()) {
                toolbar.getItems().add(new Separator());
            }
            
            // Add the button to toolbar
            toolbar.getItems().add(pathScopeButton);
        }
    }

    @Override
    public String getName() {
        return "PathScope Integration";
    }

    @Override
    public String getDescription() {
        return "Integration with PathScope API for pathology task management and annotation.";
    }

    @Override
    public Version getQuPathVersion() {
        return Version.parse("0.5.0");
    }
}