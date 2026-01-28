package qupath.extension.pathscope.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存管理器，负责保存和加载任务信息
 */
public class CacheManager {

    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    private final Gson gson;

    public CacheManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        // 确保缓存目录存在
        ensureCacheDirExists();
        logger.debug("CacheManager initialized");
    }

    /**
     * 获取缓存目录路径
     */
    private String getCacheDir() {
        return Configuration.getInstance().getCachePath();
    }

    /**
     * 获取任务缓存文件路径
     */
    private String getTasksCacheFile() {
        return getCacheDir() + "/tasks.json";
    }

    /**
     * 获取单个任务的缓存目录路径
     */
    public String getTaskCacheDir(String taskId) {
        return getCacheDir() + "/tasks/" + taskId;
    }

    /**
     * 获取单个任务的详情缓存文件路径
     */
    public String getTaskDetailsCacheFile(String taskId) {
        return getTaskCacheDir(taskId) + "/task.json";
    }

    /**
     * 获取单个任务的WSI列表缓存文件路径
     */
    public String getTaskWsiListCacheFile(String taskId) {
        return getTaskCacheDir(taskId) + "/wsi_list.json";
    }

    /**
     * 获取WSI缓存目录路径
     */
    public String getWsiCacheDir(String taskId) {
        return getCacheDir() + "/wsi/" + taskId;
    }

    /**
     * 获取单个WSI文件的缓存路径
     */
    public String getWsiCachePath(String taskId, String wsiId) {
        return getWsiCacheDir(taskId) + "/" + wsiId + ".tif";
    }

    /**
     * 确保缓存目录存在
     */
    private void ensureCacheDirExists() {
        File cacheDir = new File(getCacheDir());
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        
        // 确保任务目录存在
        File tasksDir = new File(getCacheDir() + "/tasks");
        if (!tasksDir.exists()) {
            tasksDir.mkdirs();
        }
        
        // 确保WSI目录存在
        File wsiDir = new File(getCacheDir() + "/wsi");
        if (!wsiDir.exists()) {
            wsiDir.mkdirs();
        }
    }

    /**
     * 确保任务缓存目录存在
     */
    public void ensureTaskCacheDirExists(String taskId) {
        File taskDir = new File(getTaskCacheDir(taskId));
        if (!taskDir.exists()) {
            taskDir.mkdirs();
        }
    }

    /**
     * 确保WSI缓存目录存在
     */
    public void ensureWsiCacheDirExists(String taskId) {
        File wsiDir = new File(getWsiCacheDir(taskId));
        if (!wsiDir.exists()) {
            wsiDir.mkdirs();
        }
    }

    /**
     * 保存任务列表到缓存
     */
    public void saveTaskList(List<Task> tasks, int total) throws IOException {
        logger.debug("Saving task list to cache: {} (total: {})", tasks.size(), total);
        // 确保缓存目录存在
        ensureCacheDirExists();
        
        // 保存任务列表到主缓存文件
        File cacheFile = new File(getTasksCacheFile());
        try (FileWriter writer = new FileWriter(cacheFile)) {
            JsonObject tasksObj = new JsonObject();
            tasksObj.addProperty("total", total);
            
            JsonArray tasksArray = new JsonArray();
            for (Task task : tasks) {
                JsonObject taskJson = new JsonObject();
                taskJson.addProperty("id", task.getId());
                taskJson.addProperty("name", task.getName());
                taskJson.addProperty("description", task.getDescription());
                taskJson.addProperty("num_wsi", task.getNumWsi());
                taskJson.addProperty("progress", task.getProgress());
                taskJson.addProperty("status", task.getStatus());
                tasksArray.add(taskJson);
            }
            
            tasksObj.add("items", tasksArray);
            gson.toJson(tasksObj, writer);
            logger.info("Task list saved to cache: {} (total: {})", tasks.size(), total);
        }
        
        // 为每个任务创建单独的缓存目录和文件
        for (Task task : tasks) {
            saveTaskDetails(task);
        }
    }
    
    /**
     * 保存任务列表到缓存（兼容旧版本，默认total为列表大小）
     */
    public void saveTaskList(List<Task> tasks) throws IOException {
        saveTaskList(tasks, tasks.size());
    }

    /**
     * 保存单个任务的详情到缓存
     */
    public void saveTaskDetails(Task task) throws IOException {
        logger.debug("Saving task details to cache: {}", task.getId());
        // 确保任务缓存目录存在
        ensureTaskCacheDirExists(task.getId());
        
        // 保存任务详情
        File taskFile = new File(getTaskDetailsCacheFile(task.getId()));
        try (FileWriter writer = new FileWriter(taskFile)) {
            JsonObject taskJson = new JsonObject();
            taskJson.addProperty("id", task.getId());
            taskJson.addProperty("name", task.getName());
            taskJson.addProperty("description", task.getDescription());
            
            if (task.getProject() != null) {
                JsonObject projectJson = new JsonObject();
                projectJson.addProperty("id", task.getProject().getId());
                projectJson.addProperty("name", task.getProject().getName());
                taskJson.add("project_obj", projectJson);
            }
            
            taskJson.addProperty("num_wsi", task.getNumWsi());
            taskJson.addProperty("progress", task.getProgress());
            taskJson.addProperty("status", task.getStatus());
            gson.toJson(taskJson, writer);
            logger.info("Task details saved to cache: {}", task.getId());
        }
    }

    /**
     * 保存任务的WSI列表到缓存
     */
    public void saveTaskWsiList(String taskId, List<TaskFile> wsiList, int total) throws IOException {
        logger.debug("Saving WSI list to cache for task {}: {} (total: {})", taskId, wsiList.size(), total);
        // 确保任务缓存目录存在
        ensureTaskCacheDirExists(taskId);
        
        // 保存WSI列表
        File wsiListFile = new File(getTaskWsiListCacheFile(taskId));
        try (FileWriter writer = new FileWriter(wsiListFile)) {
            JsonObject wsiObj = new JsonObject();
            wsiObj.addProperty("total", total);
            
            JsonArray wsiArray = new JsonArray();
            for (TaskFile taskFile : wsiList) {
                JsonObject wsiJson = new JsonObject();
                wsiJson.addProperty("id", taskFile.getId());
                
                // 获取文件名（从wsi对象）
                if (taskFile.getWsi() != null) {
                    wsiJson.addProperty("name", taskFile.getWsi().getName());
                }
                
                wsiJson.addProperty("type", taskFile.getType());
                wsiJson.addProperty("status_local", taskFile.getLocalStatus());
                wsiJson.addProperty("is_annotated", taskFile.isAnnotated());
                wsiJson.addProperty("local_path", taskFile.getLocalPath());
                
                if (taskFile.getWsi() != null) {
                    JsonObject wsiObjJson = new JsonObject();
                    wsiObjJson.addProperty("id", taskFile.getWsi().getId());
                    wsiObjJson.addProperty("name", taskFile.getWsi().getName());
                    wsiObjJson.addProperty("file_path", taskFile.getWsi().getPath());
                    wsiObjJson.addProperty("status", taskFile.getWsi().getStatus().getValue());
                    wsiObjJson.addProperty("size", taskFile.getWsi().getSize());
                    wsiObjJson.addProperty("ptype", taskFile.getWsi().getPtype());
                    wsiObjJson.addProperty("download_url", taskFile.getWsi().getDownloadUrl());
                    wsiJson.add("wsi", wsiObjJson);
                }
                
                wsiArray.add(wsiJson);
            }
            
            wsiObj.add("items", wsiArray);
            gson.toJson(wsiObj, writer);
            logger.info("WSI list saved to cache for task {}: {} (total: {})", taskId, wsiList.size(), total);
        }
    }
    
    /**
     * 保存任务的WSI列表到缓存（兼容旧版本，默认total为列表大小）
     */
    public void saveTaskWsiList(String taskId, List<TaskFile> wsiList) throws IOException {
        saveTaskWsiList(taskId, wsiList, wsiList.size());
    }

    /**
     * 从缓存加载任务列表
     */
    public List<Task> loadTaskList() throws IOException {
        File cacheFile = new File(getTasksCacheFile());
        if (!cacheFile.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(cacheFile)) {
            JsonElement rootElement = gson.fromJson(reader, JsonElement.class);
            List<Task> tasks = new ArrayList<>();
            
            if (rootElement.isJsonObject()) {
                // 新格式：包含total和items
                JsonObject tasksObj = rootElement.getAsJsonObject();
                JsonArray tasksArray = tasksObj.getAsJsonArray("items");
                
                for (int i = 0; i < tasksArray.size(); i++) {
                    JsonObject taskJson = tasksArray.get(i).getAsJsonObject();
                    Task task = Task.fromJson(taskJson, this);
                    tasks.add(task);
                }
            } else if (rootElement.isJsonArray()) {
                // 旧格式：直接是任务数组
                JsonArray tasksArray = rootElement.getAsJsonArray();
                
                for (int i = 0; i < tasksArray.size(); i++) {
                    JsonObject taskJson = tasksArray.get(i).getAsJsonObject();
                    Task task = Task.fromJson(taskJson, this);
                    tasks.add(task);
                }
            }
            
            return tasks;
        }
    }
    
    /**
     * 从缓存加载任务总数
     */
    public int loadTaskTotal() throws IOException {
        File cacheFile = new File(getTasksCacheFile());
        if (!cacheFile.exists()) {
            return 0;
        }

        try (FileReader reader = new FileReader(cacheFile)) {
            JsonElement rootElement = gson.fromJson(reader, JsonElement.class);
            
            if (rootElement.isJsonObject()) {
                // 新格式：包含total
                JsonObject tasksObj = rootElement.getAsJsonObject();
                if (tasksObj.has("total")) {
                    return tasksObj.get("total").getAsInt();
                }
            }
            
            return 0;
        }
    }



    /**
     * 从缓存加载单个任务的详情
     */
    public Task loadTaskDetails(String taskId) throws IOException {
        File taskFile = new File(getTaskDetailsCacheFile(taskId));
        if (!taskFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(taskFile)) {
            JsonObject taskJson = gson.fromJson(reader, JsonObject.class);
            return Task.fromJson(taskJson, this);
        }
    }

    /**
     * 从缓存加载任务的WSI列表
     */
    public List<TaskFile> loadTaskWsiList(String taskId) throws IOException {
        File wsiListFile = new File(getTaskWsiListCacheFile(taskId));
        if (!wsiListFile.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(wsiListFile)) {
            JsonElement rootElement = gson.fromJson(reader, JsonElement.class);
            List<TaskFile> wsiList = new ArrayList<>();
            
            if (rootElement.isJsonObject()) {
                // 新格式：包含total和items
                JsonObject wsiObj = rootElement.getAsJsonObject();
                JsonArray wsiArray = wsiObj.getAsJsonArray("items");
                
                for (int i = 0; i < wsiArray.size(); i++) {
                    JsonObject wsiJson = wsiArray.get(i).getAsJsonObject();
                    TaskFile taskFile = TaskFile.fromJson(wsiJson, null, this);
                    wsiList.add(taskFile);
                }
            } else if (rootElement.isJsonArray()) {
                // 旧格式：直接是WSI数组
                JsonArray wsiArray = rootElement.getAsJsonArray();
                
                for (int i = 0; i < wsiArray.size(); i++) {
                    JsonObject wsiJson = wsiArray.get(i).getAsJsonObject();
                    TaskFile taskFile = TaskFile.fromJson(wsiJson, null, this);
                    wsiList.add(taskFile);
                }
            }
            
            return wsiList;
        }
    }

    /**
     * 从缓存加载指定页的WSI列表
     * 
     * @param taskId   任务ID
     * @param page     页码（从1开始）
     * @param pageSize 每页数量
     * @return 指定页的数据列表，如果不在缓存中则返回空列表
     */
    public List<TaskFile> loadTaskWsiListPage(String taskId, int page, int pageSize) throws IOException {
        File pageCacheFile = new File(getTaskWsiPageCacheFile(taskId, page, pageSize));
        if (!pageCacheFile.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(pageCacheFile)) {
            JsonElement rootElement = gson.fromJson(reader, JsonElement.class);
            List<TaskFile> wsiList = new ArrayList<>();
            
            if (rootElement.isJsonObject()) {
                JsonObject wsiObj = rootElement.getAsJsonObject();
                JsonArray wsiArray = wsiObj.getAsJsonArray("items");
                
                for (int i = 0; i < wsiArray.size(); i++) {
                    JsonObject wsiJson = wsiArray.get(i).getAsJsonObject();
                    TaskFile taskFile = TaskFile.fromJson(wsiJson, null, this);
                    wsiList.add(taskFile);
                }
            }
            
            return wsiList;
        }
    }

    /**
     * 获取指定页的缓存文件路径
     */
    private String getTaskWsiPageCacheFile(String taskId, int page, int pageSize) {
        return getTaskCacheDir(taskId) + "/wsi_list_page_" + page + "_" + pageSize + ".json";
    }

    /**
     * 保存任务的WSI列表到缓存（按页存储）
     * 
     * @param taskId   任务ID
     * @param page     页码（从1开始）
     * @param pageSize 每页数量
     * @param wsiList  WSI列表
     */
    public void saveTaskWsiListPage(String taskId, int page, int pageSize, List<TaskFile> wsiList) throws IOException {
        ensureTaskCacheDirExists(taskId);
        
        File pageCacheFile = new File(getTaskWsiPageCacheFile(taskId, page, pageSize));
        try (FileWriter writer = new FileWriter(pageCacheFile)) {
            JsonObject wsiObj = new JsonObject();
            
            JsonArray wsiArray = new JsonArray();
            for (TaskFile taskFile : wsiList) {
                JsonObject wsiJson = new JsonObject();
                wsiJson.addProperty("id", taskFile.getId());
                
                if (taskFile.getWsi() != null) {
                    wsiJson.addProperty("name", taskFile.getWsi().getName());
                }
                
                wsiJson.addProperty("type", taskFile.getType());
                wsiJson.addProperty("status_local", taskFile.getLocalStatus());
                wsiJson.addProperty("is_annotated", taskFile.isAnnotated());
                wsiJson.addProperty("local_path", taskFile.getLocalPath());
                
                if (taskFile.getWsi() != null) {
                    JsonObject wsiObjJson = new JsonObject();
                    wsiObjJson.addProperty("id", taskFile.getWsi().getId());
                    wsiObjJson.addProperty("name", taskFile.getWsi().getName());
                    wsiObjJson.addProperty("file_path", taskFile.getWsi().getPath());
                    wsiObjJson.addProperty("status", taskFile.getWsi().getStatus().getValue());
                    wsiObjJson.addProperty("size", taskFile.getWsi().getSize());
                    wsiObjJson.addProperty("ptype", taskFile.getWsi().getPtype());
                    wsiObjJson.addProperty("download_url", taskFile.getWsi().getDownloadUrl());
                    wsiJson.add("wsi", wsiObjJson);
                }
                
                wsiArray.add(wsiJson);
            }
            
            wsiObj.add("items", wsiArray);
            gson.toJson(wsiObj, writer);
            logger.debug("Saved WSI list page {} to cache for task {}: {} items", page, taskId, wsiList.size());
        }
    }

    /**
     * 保存任务WSI总数到缓存
     */
    public void saveTaskWsiTotal(String taskId, int total) throws IOException {
        ensureTaskCacheDirExists(taskId);
        
        File totalFile = new File(getTaskCacheDir(taskId) + "/wsi_total.json");
        try (FileWriter writer = new FileWriter(totalFile)) {
            JsonObject obj = new JsonObject();
            obj.addProperty("total", total);
            gson.toJson(obj, writer);
        }
    }
    
    /**
     * 从缓存加载任务的WSI总数
     */
    public int loadTaskWsiTotal(String taskId) throws IOException {
        File wsiListFile = new File(getTaskWsiListCacheFile(taskId));
        if (!wsiListFile.exists()) {
            return 0;
        }

        try (FileReader reader = new FileReader(wsiListFile)) {
            JsonElement rootElement = gson.fromJson(reader, JsonElement.class);
            
            if (rootElement.isJsonObject()) {
                // 新格式：包含total
                JsonObject wsiObj = rootElement.getAsJsonObject();
                if (wsiObj.has("total")) {
                    return wsiObj.get("total").getAsInt();
                }
            }
            
            return 0;
        }
    }

    /**
     * 检查WSI文件是否已缓存
     */
    public boolean isWsiCached(String taskId, String wsiId) {
        File wsiFile = new File(getWsiCachePath(taskId, wsiId));
        return wsiFile.exists();
    }

    /**
     * 保存单个任务到缓存
     */
    public void saveTask(Task task) throws IOException {
        logger.debug("Saving task to cache: {}", task.getId());
        saveTaskDetails(task);
        // 保存任务的WSI列表
        if (task.getFiles() != null && !task.getFiles().isEmpty()) {
            saveTaskWsiList(task.getId(), task.getFiles());
        }
        logger.info("Task saved to cache: {}", task.getId());
    }

    /**
     * 清除缓存
     */
    public void clearCache() throws IOException {
        File cacheDir = new File(getCacheDir());
        deleteDirectory(cacheDir);
        ensureCacheDirExists();
    }

    /**
     * 删除目录及其所有内容
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    /**
     * 保存全部任务到缓存
     */
    public void saveAllTasks(List<Task> tasks) throws IOException {
        logger.debug("Saving all tasks to cache: {} tasks", tasks.size());
        ensureCacheDirExists();
        
        File cacheFile = new File(getCacheDir() + "/all_tasks.json");
        try (FileWriter writer = new FileWriter(cacheFile)) {
            JsonObject tasksObj = new JsonObject();
            tasksObj.addProperty("total", tasks.size());
            
            JsonArray tasksArray = new JsonArray();
            for (Task task : tasks) {
                JsonObject taskJson = new JsonObject();
                taskJson.addProperty("id", task.getId());
                taskJson.addProperty("name", task.getName());
                taskJson.addProperty("description", task.getDescription());
                taskJson.addProperty("num_wsi", task.getNumWsi());
                taskJson.addProperty("progress", task.getProgress());
                taskJson.addProperty("status", task.getStatus());
                
                if (task.getProject() != null) {
                    JsonObject projectJson = new JsonObject();
                    projectJson.addProperty("id", task.getProject().getId());
                    projectJson.addProperty("name", task.getProject().getName());
                    taskJson.add("project", projectJson);
                }
                
                tasksArray.add(taskJson);
            }
            
            tasksObj.add("items", tasksArray);
            gson.toJson(tasksObj, writer);
            logger.info("All tasks saved to cache: {} tasks", tasks.size());
        }
    }

    /**
     * 从缓存加载全部任务
     */
    public List<Task> loadAllTasks() throws IOException {
        File cacheFile = new File(getCacheDir() + "/all_tasks.json");
        if (!cacheFile.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(cacheFile)) {
            JsonElement rootElement = gson.fromJson(reader, JsonElement.class);
            List<Task> tasks = new ArrayList<>();
            
            if (rootElement.isJsonObject()) {
                JsonObject tasksObj = rootElement.getAsJsonObject();
                JsonArray tasksArray = tasksObj.getAsJsonArray("items");
                
                for (int i = 0; i < tasksArray.size(); i++) {
                    JsonObject taskJson = tasksArray.get(i).getAsJsonObject();
                    Task task = Task.fromJson(taskJson, this);
                    tasks.add(task);
                }
            }
            
            logger.debug("Loaded all tasks from cache: {} tasks", tasks.size());
            return tasks;
        }
    }

    /**
     * 保存任务的全部 WSI 数据到缓存
     */
    public void saveAllTaskWsi(String taskId, List<TaskFile> wsiList) throws IOException {
        ensureTaskCacheDirExists(taskId);
        
        File cacheFile = new File(getTaskCacheDir(taskId) + "/all_wsi.json");
        try (FileWriter writer = new FileWriter(cacheFile)) {
            JsonObject wsiObj = new JsonObject();
            wsiObj.addProperty("total", wsiList.size());
            
            JsonArray wsiArray = new JsonArray();
            for (TaskFile taskFile : wsiList) {
                JsonObject wsiJson = new JsonObject();
                wsiJson.addProperty("id", taskFile.getId());
                
                if (taskFile.getWsi() != null) {
                    wsiJson.addProperty("name", taskFile.getWsi().getName());
                }
                
                wsiJson.addProperty("type", taskFile.getType());
                wsiJson.addProperty("status_local", taskFile.getLocalStatus());
                wsiJson.addProperty("is_annotated", taskFile.isAnnotated());
                wsiJson.addProperty("local_path", taskFile.getLocalPath());
                
                if (taskFile.getWsi() != null) {
                    JsonObject wsiObjJson = new JsonObject();
                    wsiObjJson.addProperty("id", taskFile.getWsi().getId());
                    wsiObjJson.addProperty("name", taskFile.getWsi().getName());
                    wsiObjJson.addProperty("file_path", taskFile.getWsi().getPath());
                    wsiObjJson.addProperty("status", taskFile.getWsi().getStatus().getValue());
                    wsiObjJson.addProperty("size", taskFile.getWsi().getSize());
                    wsiObjJson.addProperty("ptype", taskFile.getWsi().getPtype());
                    wsiObjJson.addProperty("download_url", taskFile.getWsi().getDownloadUrl());
                    wsiJson.add("wsi", wsiObjJson);
                }
                
                wsiArray.add(wsiJson);
            }
            
            wsiObj.add("items", wsiArray);
            gson.toJson(wsiObj, writer);
            logger.info("All WSI saved to cache for task {}: {} items", taskId, wsiList.size());
        }
    }

    /**
     * 从缓存加载任务的全部 WSI 数据
     */
    public List<TaskFile> loadAllTaskWsi(String taskId) throws IOException {
        File cacheFile = new File(getTaskCacheDir(taskId) + "/all_wsi.json");
        if (!cacheFile.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(cacheFile)) {
            JsonElement rootElement = gson.fromJson(reader, JsonElement.class);
            List<TaskFile> wsiList = new ArrayList<>();

            if (rootElement.isJsonObject()) {
                JsonObject wsiObj = rootElement.getAsJsonObject();
                JsonArray wsiArray = wsiObj.getAsJsonArray("items");

                for (int i = 0; i < wsiArray.size(); i++) {
                    JsonObject wsiJson = wsiArray.get(i).getAsJsonObject();
                    TaskFile taskFile = TaskFile.fromJson(wsiJson, null, this);
                    wsiList.add(taskFile);
                }
            }

            logger.debug("Loaded all WSI from cache for task {}: {} items", taskId, wsiList.size());
            return wsiList;
        }
    }

    /**
     * 更新单个 WSI 的 local status 到缓存
     * 这个方法只更新 WSI 的 local status 字段，不会重新保存整个任务
     *
     * @param taskId      任务ID
     * @param wsiId       WSI ID
     * @param localStatus 新的本地状态
     * @return 是否更新成功
     */
    public boolean updateWsiLocalStatus(String taskId, String wsiId, String localStatus) {
        logger.debug("Updating WSI local status in cache: taskId={}, wsiId={}, status={}", taskId, wsiId, localStatus);

        boolean updated = false;

        // 1. 更新主 WSI 列表文件 (wsi_list.json)
        try {
            updated = updateWsiStatusInFile(getTaskWsiListCacheFile(taskId), wsiId, localStatus) || updated;
        } catch (IOException e) {
            logger.warn("Failed to update WSI status in main list: {}", e.getMessage());
        }

        // 2. 更新全部 WSI 文件 (all_wsi.json)
        try {
            String allWsiFile = getTaskCacheDir(taskId) + "/all_wsi.json";
            updated = updateWsiStatusInFile(allWsiFile, wsiId, localStatus) || updated;
        } catch (IOException e) {
            logger.warn("Failed to update WSI status in all_wsi.json: {}", e.getMessage());
        }

        // 3. 更新所有分页缓存文件
        File taskDir = new File(getTaskCacheDir(taskId));
        if (taskDir.exists() && taskDir.isDirectory()) {
            File[] pageFiles = taskDir.listFiles((dir, name) -> name.startsWith("wsi_list_page_") && name.endsWith(".json"));
            if (pageFiles != null) {
                for (File pageFile : pageFiles) {
                    try {
                        updated = updateWsiStatusInFile(pageFile.getAbsolutePath(), wsiId, localStatus) || updated;
                    } catch (IOException e) {
                        logger.warn("Failed to update WSI status in page file {}: {}", pageFile.getName(), e.getMessage());
                    }
                }
            }
        }

        if (updated) {
            logger.info("Successfully updated WSI local status in cache: taskId={}, wsiId={}, status={}", taskId, wsiId, localStatus);
        } else {
            logger.warn("WSI not found in any cache file: taskId={}, wsiId={}", taskId, wsiId);
        }

        return updated;
    }

    /**
     * 更新单个 WSI 的本地路径到缓存
     *
     * @param taskId    任务ID
     * @param wsiId     WSI ID
     * @param localPath 新的本地路径
     * @return 是否更新成功
     */
    public boolean updateWsiLocalPath(String taskId, String wsiId, String localPath) {
        logger.debug("Updating WSI local path in cache: taskId={}, wsiId={}, path={}", taskId, wsiId, localPath);

        boolean updated = false;

        // 更新主 WSI 列表文件
        try {
            updated = updateWsiFieldInFile(getTaskWsiListCacheFile(taskId), wsiId, "local_path", localPath) || updated;
        } catch (IOException e) {
            logger.warn("Failed to update WSI path in main list: {}", e.getMessage());
        }

        // 更新全部 WSI 文件
        try {
            String allWsiFile = getTaskCacheDir(taskId) + "/all_wsi.json";
            updated = updateWsiFieldInFile(allWsiFile, wsiId, "local_path", localPath) || updated;
        } catch (IOException e) {
            logger.warn("Failed to update WSI path in all_wsi.json: {}", e.getMessage());
        }

        // 更新所有分页缓存文件
        File taskDir = new File(getTaskCacheDir(taskId));
        if (taskDir.exists() && taskDir.isDirectory()) {
            File[] pageFiles = taskDir.listFiles((dir, name) -> name.startsWith("wsi_list_page_") && name.endsWith(".json"));
            if (pageFiles != null) {
                for (File pageFile : pageFiles) {
                    try {
                        updated = updateWsiFieldInFile(pageFile.getAbsolutePath(), wsiId, "local_path", localPath) || updated;
                    } catch (IOException e) {
                        logger.warn("Failed to update WSI path in page file {}: {}", pageFile.getName(), e.getMessage());
                    }
                }
            }
        }

        if (updated) {
            logger.info("Successfully updated WSI local path in cache: taskId={}, wsiId={}", taskId, wsiId);
        }

        return updated;
    }

    /**
     * 在指定的缓存文件中更新 WSI 的 local status
     *
     * @param filePath    缓存文件路径
     * @param wsiId       WSI ID
     * @param localStatus 新的本地状态
     * @return 是否找到并更新了该 WSI
     */
    private boolean updateWsiStatusInFile(String filePath, String wsiId, String localStatus) throws IOException {
        return updateWsiFieldInFile(filePath, wsiId, "status_local", localStatus);
    }

    /**
     * 在指定的缓存文件中更新 WSI 的字段
     *
     * @param filePath  缓存文件路径
     * @param wsiId     WSI ID
     * @param fieldName 字段名
     * @param value     新值
     * @return 是否找到并更新了该 WSI
     */
    private boolean updateWsiFieldInFile(String filePath, String wsiId, String fieldName, Object value) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }

        boolean found = false;

        // 读取文件内容
        JsonElement rootElement;
        try (FileReader reader = new FileReader(file)) {
            rootElement = gson.fromJson(reader, JsonElement.class);
        }

        if (rootElement == null || !rootElement.isJsonObject()) {
            return false;
        }

        JsonObject root = rootElement.getAsJsonObject();
        JsonArray items = root.getAsJsonArray("items");

        if (items == null) {
            return false;
        }

        // 查找并更新对应的 WSI
        for (int i = 0; i < items.size(); i++) {
            JsonObject wsiJson = items.get(i).getAsJsonObject();
            if (wsiJson.has("id") && wsiId.equals(wsiJson.get("id").getAsString())) {
                // 根据值的类型添加到JSON
                if (value instanceof String) {
                    wsiJson.addProperty(fieldName, (String) value);
                } else if (value instanceof Number) {
                    wsiJson.addProperty(fieldName, (Number) value);
                } else if (value instanceof Boolean) {
                    wsiJson.addProperty(fieldName, (Boolean) value);
                }
                found = true;
                break;
            }
        }

        // 如果找到并更新了，写回文件
        if (found) {
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(root, writer);
            }
        }

        return found;
    }
}
