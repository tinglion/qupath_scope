package qupath.extension.pathscope.ui;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
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
        
        // Add PathScope dropdown menus to toolbar
        var toolbar = qupath.getToolBar();
        if (toolbar != null) {
            if (!toolbar.getItems().isEmpty()) {
                toolbar.getItems().add(new Separator());
            }

            // PathScope dropdown menu
            MenuButton pathScopeMenu = new MenuButton("PathScope");
            pathScopeMenu.getItems().addAll(
                    createMenuItem(pathScopeCommands.actionPathScope),
                    createMenuItem(pathScopeCommands.actionLogin),
                    createMenuItem(pathScopeCommands.actionGetTaskList),
                    createMenuItem(pathScopeCommands.actionConfiguration),
                    createMenuItem(pathScopeCommands.actionExpertAssessment)
            );

            // Zoom dropdown menu
            var zoomCommands = pathScopeCommands.zoomCommands;
            MenuButton zoomMenu = new MenuButton("Zoom");
            zoomMenu.getItems().addAll(
                    createMenuItem(zoomCommands.actionZoomFit),
                    createMenuItem(zoomCommands.actionZoom5x),
                    createMenuItem(zoomCommands.actionZoom10x),
                    createMenuItem(zoomCommands.actionZoom20x),
                    createMenuItem(zoomCommands.actionZoom40x),
                    createMenuItem(zoomCommands.actionZoomOrigin)
            );

            toolbar.getItems().addAll(pathScopeMenu, zoomMenu);
        }
    }

    private static MenuItem createMenuItem(org.controlsfx.control.action.Action action) {
        MenuItem item = new MenuItem();
        item.textProperty().bind(action.textProperty());
        item.setOnAction(e -> action.handle(new javafx.event.ActionEvent()));
        item.disableProperty().bind(action.disabledProperty());
        return item;
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