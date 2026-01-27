package qupath.extension.pathscope.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import qupath.extension.pathscope.data.Configuration;

import java.io.File;
import java.io.IOException;

/**
 * 配置对话框，允许用户设置缓存路径等配置
 */
public class ConfigurationDialog {

    private Stage dialogStage;
    private TextField cachePathField;
    private TextField apiBaseUrlField;
    private Configuration config;
    private boolean saved = false;
    
    /**
     * 构造函数
     */
    public ConfigurationDialog() {
        this.config = Configuration.getInstance();
    }
    
    /**
     * 显示配置对话框
     * @return 是否保存了配置
     */
    public boolean showAndWait() {
        // 创建对话框舞台
        dialogStage = new Stage();
        dialogStage.setTitle("PathScope 配置");
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        
        // 创建布局
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // 缓存路径设置
        Label cachePathLabel = new Label("缓存路径:");
        grid.add(cachePathLabel, 0, 0);
        
        cachePathField = new TextField(config.getCachePath());
        cachePathField.setPrefWidth(400);
        grid.add(cachePathField, 1, 0);
        
        Button browseButton = new Button("浏览...");
        browseButton.setOnAction(e -> browseCachePath());
        grid.add(browseButton, 2, 0);
        
        // API基础URL设置
        Label apiBaseUrlLabel = new Label("API基础URL:");
        grid.add(apiBaseUrlLabel, 0, 1);
        
        apiBaseUrlField = new TextField(config.getApiBaseUrl());
        apiBaseUrlField.setPrefWidth(400);
        grid.add(apiBaseUrlField, 1, 1);
        
        // 按钮区域
        Button saveButton = new Button("保存");
        saveButton.setOnAction(e -> saveConfiguration());
        
        Button cancelButton = new Button("取消");
        cancelButton.setOnAction(e -> dialogStage.close());
        
        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(10);
        buttonGrid.add(saveButton, 0, 0);
        buttonGrid.add(cancelButton, 1, 0);
        grid.add(buttonGrid, 1, 2);
        
        // 设置场景
        Scene scene = new Scene(grid);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
        
        return saved;
    }
    
    /**
     * 浏览缓存路径
     */
    private void browseCachePath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择缓存目录");
        
        // 设置初始目录为当前缓存路径
        File currentDir = new File(cachePathField.getText());
        if (currentDir.exists() && currentDir.isDirectory()) {
            directoryChooser.setInitialDirectory(currentDir);
        }
        
        File selectedDir = directoryChooser.showDialog(dialogStage);
        if (selectedDir != null) {
            cachePathField.setText(selectedDir.getAbsolutePath());
        }
    }
    
    /**
     * 保存配置
     */
    private void saveConfiguration() {
        try {
            // 更新配置
            config.setCachePath(cachePathField.getText());
            config.setApiBaseUrl(apiBaseUrlField.getText());
            config.save();
            
            saved = true;
            dialogStage.close();
        } catch (IOException e) {
            // 显示错误信息
            AlertDialog.showError("保存配置失败", "无法保存配置文件: " + e.getMessage());
        }
    }
}