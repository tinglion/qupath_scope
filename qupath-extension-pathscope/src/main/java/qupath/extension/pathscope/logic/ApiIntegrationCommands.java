package qupath.extension.pathscope.logic;

import org.controlsfx.control.action.Action;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.actions.ActionTools;
import qupath.lib.gui.actions.annotations.ActionMenu;

import qupath.extension.pathscope.ui.LoginDialog;
import qupath.extension.pathscope.ui.TaskListDialog;
import qupath.extension.pathscope.ui.ConfigurationDialog;
import qupath.extension.pathscope.ui.ExpertAssessmentDialog;

/**
 * Commands for API integration.
 */
public class ApiIntegrationCommands {

    private static ApiClient apiClient;

    @ActionMenu("PathScope")
    public static class PathScopeCommands {

        public final Action actionPathScope;
        public final Action actionLogin;
        public final Action actionGetTaskList;
        public final Action actionConfiguration;
        public final Action actionExpertAssessment;

        public PathScopeCommands(QuPathGUI qupath) {
            apiClient = new ApiClient();

            actionPathScope = ActionTools.createAction(() -> showPathScopeMain(qupath), "PathScope");
            actionLogin = ActionTools.createAction(() -> showLoginDialog(qupath), "Login");
            actionGetTaskList = ActionTools.createAction(() -> showTaskListDialog(qupath), "Tasks");
            actionConfiguration = ActionTools.createAction(() -> showConfigurationDialog(), "Settings");
            actionExpertAssessment = ActionTools.createAction(() -> showExpertAssessmentDialog(qupath), "Expert Assessment");
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
}
