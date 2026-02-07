package qupath.extension.pathscope.logic;

import org.controlsfx.control.action.Action;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.actions.ActionTools;
import qupath.lib.gui.actions.annotations.ActionMenu;
import qupath.lib.gui.viewer.QuPathViewer;

import qupath.extension.pathscope.ui.LoginDialog;
import qupath.extension.pathscope.ui.TaskListDialog;
import qupath.extension.pathscope.ui.ConfigurationDialog;
import qupath.extension.pathscope.ui.ExpertAssessmentDialog;

/**
 * Commands for API integration.
 */
public class ApiIntegrationCommands {

    private static ApiClient apiClient;
    private static ZoomCommands lastZoomCommands;

    @ActionMenu("PathScope")
    public static class PathScopeCommands {

        public final Action actionPathScope;
        public final Action actionLogin;
        public final Action actionGetTaskList;
        public final Action actionConfiguration;
        public final Action actionExpertAssessment;

        // 缩放子菜单
        @ActionMenu("Zoom")
        public final ZoomCommands zoomCommands;

        public PathScopeCommands(QuPathGUI qupath) {
            apiClient = new ApiClient();

            actionPathScope = ActionTools.createAction(() -> showPathScopeMain(qupath), "PathScope");
            actionLogin = ActionTools.createAction(() -> showLoginDialog(qupath), "Login");
            actionGetTaskList = ActionTools.createAction(() -> showTaskListDialog(qupath), "Tasks");
            actionConfiguration = ActionTools.createAction(() -> showConfigurationDialog(), "Settings");
            actionExpertAssessment = ActionTools.createAction(() -> showExpertAssessmentDialog(qupath), "Expert Assessment");

            zoomCommands = new ZoomCommands(qupath);
        }
    }

    /**
     * 缩放命令集合，作为PathScope菜单下的Zoom子菜单
     */
    public static class ZoomCommands {
        public final Action actionZoomFit;
        public final Action actionZoom5x;
        public final Action actionZoom10x;
        public final Action actionZoom20x;
        public final Action actionZoom40x;
        public final Action actionZoomOrigin;

        public ZoomCommands(QuPathGUI qupath) {
            actionZoomFit = ActionTools.createAction(() -> zoomToFit(qupath), "Fit (Window)");
            actionZoom5x = ActionTools.createAction(() -> zoomToMultiplier(qupath, 5), "5x (Fit)");
            actionZoom10x = ActionTools.createAction(() -> zoomToMultiplier(qupath, 10), "10x (Fit)");
            actionZoom20x = ActionTools.createAction(() -> zoomToMultiplier(qupath, 20), "20x (Fit)");
            actionZoom40x = ActionTools.createAction(() -> zoomToMultiplier(qupath, 40), "40x (Fit)");
            actionZoomOrigin = ActionTools.createAction(() -> zoomToOrigin(qupath), "Origin");
            lastZoomCommands = this;
        }
    }

    /**
     * 主入口方法，点击PathScope菜单时调用
     * 先检查登录状态，未登录则显示登录对话框，登录成功后自动打开任务列表
     */
    private static void showPathScopeMain(QuPathGUI qupath) {
        // 检查是否已登录（通过检查apiClient是否有token）
        if (apiClient == null || apiClient.getAuthToken() == null || apiClient.getAuthToken().isEmpty()) {
            // 未登录，显示登录对话框
            LoginDialog loginDialog = new LoginDialog(qupath, apiClient);
            boolean loginSuccess = loginDialog.showAndWait();
            if (loginSuccess) {
                // 登录成功，自动打开任务列表
                showTaskListDialog(qupath);
            }
        } else {
            // 已登录，直接打开任务列表
            showTaskListDialog(qupath);
        }
    }

    private static void showLoginDialog(QuPathGUI qupath) {
        LoginDialog dialog = new LoginDialog(qupath, apiClient);
        dialog.showAndWait();
    }

    private static void showTaskListDialog(QuPathGUI qupath) {
        if (apiClient != null) {
            TaskListDialog dialog = new TaskListDialog(qupath, apiClient);
            dialog.showAndWait();
        } else {
            System.out.println("API client not initialized. Please login first.");
        }
    }

    /**
     * 显示配置对话框
     */
    private static void showConfigurationDialog() {
        ConfigurationDialog dialog = new ConfigurationDialog();
        dialog.showAndWait();
    }

    /**
     * 显示专家评估对话框
     */
    private static void showExpertAssessmentDialog(QuPathGUI qupath) {
        ExpertAssessmentDialog dialog = new ExpertAssessmentDialog(qupath, null);
        var result = dialog.showAndWait();
        result.ifPresent(assessment -> {
            // 这里可以添加评估结果的处理逻辑，例如保存到本地或上传到服务器
            System.out.println("Expert assessment submitted:");
            System.out.println(assessment.getSummary());
        });
    }

    /**
     * 适配窗口显示（Fit to Window）
     */
    private static void zoomToFit(QuPathGUI qupath) {
        QuPathViewer viewer = qupath.getViewer();
        if (viewer != null && viewer.hasServer()) {
            viewer.zoomToFit();
        }
    }

    /**
     * 按窗口适配倍数缩放
     * multiplier=5 表示分辨率是fit-to-window的5倍，即downsample = fitDownsample / 5
     */
    private static void zoomToMultiplier(QuPathGUI qupath, double multiplier) {
        QuPathViewer viewer = qupath.getViewer();
        if (viewer == null || !viewer.hasServer())
            return;

        // 计算fit-to-window的downsample
        double fullWidth = viewer.getServer().getWidth();
        double fullHeight = viewer.getServer().getHeight();
        double viewerWidth = viewer.getView().getWidth();
        double viewerHeight = viewer.getView().getHeight();
        double fitDownsample = Math.max(fullWidth / viewerWidth, fullHeight / viewerHeight);

        // 目标downsample = fitDownsample / multiplier
        double targetDownsample = fitDownsample / multiplier;
        viewer.setDownsampleFactor(targetDownsample, -1, -1);
        viewer.centerImage();
    }

    /**
     * 原始分辨率（1:1像素），并计算相当于fit的多少倍
     */
    private static void zoomToOrigin(QuPathGUI qupath) {
        QuPathViewer viewer = qupath.getViewer();
        if (viewer == null || !viewer.hasServer())
            return;

        double fullWidth = viewer.getServer().getWidth();
        double fullHeight = viewer.getServer().getHeight();
        double viewerWidth = viewer.getView().getWidth();
        double viewerHeight = viewer.getView().getHeight();
        double fitDownsample = Math.max(fullWidth / viewerWidth, fullHeight / viewerHeight);
        int multiplier = (int) Math.round(fitDownsample);

        viewer.setDownsampleFactor(1.0, -1, -1);
        viewer.centerImage();

        // 动态更新Origin按钮文本显示倍数
        var commands = lastZoomCommands;
        if (commands != null && commands.actionZoomOrigin != null) {
            commands.actionZoomOrigin.setText("Origin (" + multiplier + "x)");
        }
    }
}
