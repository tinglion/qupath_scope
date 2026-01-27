package qupath.extension.pathscope.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 配置类，存储插件配置信息
 */
public class Configuration {

    private static final String CONFIG_FILE = System.getProperty("user.home") + "/.qupath/pathscope/config.json";
    private static Configuration instance;
    
    private String cachePath; // 缓存文件路径
    private String apiBaseUrl; // API基础URL
    
    /**
     * 获取配置单例
     */
    public static synchronized Configuration getInstance() {
        if (instance == null) {
            instance = loadFromFile();
        }
        return instance;
    }
    
    /**
     * 从文件加载配置
     */
    private static Configuration loadFromFile() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                Gson gson = new Gson();
                return gson.fromJson(reader, Configuration.class);
            } catch (IOException e) {
                // 加载失败，使用默认配置
                return new Configuration();
            }
        } else {
            // 配置文件不存在，使用默认配置
            return new Configuration();
        }
    }
    
    /**
     * 构造函数，设置默认配置
     */
    private Configuration() {
        // 默认缓存路径
        this.cachePath = System.getProperty("user.home") + "/.qupath/pathscope/cache";
        // 默认API基础URL
        this.apiBaseUrl = "http://8.130.50.197:8302/api";
    }
    
    /**
     * 获取缓存路径
     */
    public String getCachePath() {
        return cachePath;
    }
    
    /**
     * 设置缓存路径
     */
    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }
    
    /**
     * 获取API基础URL
     */
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }
    
    /**
     * 设置API基础URL
     */
    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }
    
    /**
     * 保存配置到文件
     */
    public void save() throws IOException {
        // 确保配置目录存在
        File configDir = new File(CONFIG_FILE).getParentFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        // 保存配置
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(this, writer);
        }
    }
}