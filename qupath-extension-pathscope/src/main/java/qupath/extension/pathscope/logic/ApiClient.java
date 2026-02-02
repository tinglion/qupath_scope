package qupath.extension.pathscope.logic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import qupath.extension.pathscope.data.CacheManager;
import qupath.extension.pathscope.data.Configuration;
import qupath.extension.pathscope.data.Task;
import qupath.extension.pathscope.data.TaskFile;

/**
 * Client for interacting with the external API.
 */
public class ApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);

    private static final String LOGIN_ENDPOINT = "/login/";
    private static final String TASK_LIST_ENDPOINT = "/pathos/pa_project_task/";
    private static final String TASK_WSI_LIST_ENDPOINT = "/pathos/pa_project_task_wsi/";
    private static final String SUBMIT_ANNOTATION_ENDPOINT = "/pathos/pa_project_task_wsi/{id}/";

    private final OkHttpClient client;
    private final Gson gson;
    private String authToken;
    private String apiBaseUrl;

    /**
     * 分页结果封装类
     */
    public static class PaginatedResult<T> {
        private final List<T> items;
        private final int total;

        public PaginatedResult(List<T> items, int total) {
            this.items = items;
            this.total = total;
        }

        public List<T> getItems() {
            return items;
        }

        public int getTotal() {
            return total;
        }
    }

    /**
     * 任务列表分页结果
     */
    public static class TaskListResult extends PaginatedResult<Task> {
        public TaskListResult(List<Task> items, int total) {
            super(items, total);
        }
    }

    /**
     * WSI列表分页结果
     */
    public static class WsiListResult extends PaginatedResult<TaskFile> {
        public WsiListResult(List<TaskFile> items, int total) {
            super(items, total);
        }
    }

    public ApiClient() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        // 从配置中读取API基础URL
        this.apiBaseUrl = Configuration.getInstance().getApiBaseUrl();
        logger.debug("API base URL loaded from configuration: {}", apiBaseUrl);

        // Try to load token from cache on initialization
        try {
            loadTokenFromCache();
        } catch (IOException e) {
            logger.debug("Failed to load token from cache: {}", e.getMessage());
        }
    }

    /**
     * Login to the API.
     */
    public boolean login(String username, String password) throws IOException {
        logger.debug("Starting login request for user: {}", username);

        try {
            // MD5 encrypt password
            String encryptedPassword = md5Encrypt(password);
            logger.debug("Password encrypted with MD5");

            Map<String, String> credentials = Map.of(
                    "username", username,
                    "password", encryptedPassword);

            String json = gson.toJson(credentials);
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

            String url = apiBaseUrl + LOGIN_ENDPOINT;
            logger.debug("Login URL: {}", url);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                logger.debug("Login response status: {}", response.code());

                if (response.isSuccessful()) {
                    // Parse response to get token from data.access
                    String responseBody = response.body().string();
                    // logger.debug("Login response body: {}", responseBody);
                    JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);

                    if (responseJson.has("data") && !responseJson.get("data").isJsonNull()) {
                        JsonObject dataJson = responseJson.getAsJsonObject("data");
                        if (dataJson.has("access")) {
                            this.authToken = dataJson.get("access").getAsString();
                            logger.debug("Login successful, token obtained from data.access");

                            // Save token to local cache
                            saveTokenToCache(this.authToken);

                            return true;
                        } else {
                            logger.error("Login failed: 'access' field not found in response data");
                            return false;
                        }
                    } else {
                        logger.error("Login failed: 'data' field not found or null in response");
                        return false;
                    }
                } else {
                    logger.error("Login failed: {}", response.message());
                    return false;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            logger.error("MD5 encryption failed: {}", e.getMessage());
            throw new IOException("Encryption error: " + e.getMessage());
        }
    }

    /**
     * Save token to local cache
     */
    private void saveTokenToCache(String token) throws IOException {
        // Ensure cache directory exists
        File cacheDir = new File(Configuration.getInstance().getCachePath());
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        // Save token to file
        File tokenFile = new File(cacheDir, "token.txt");
        try (FileWriter writer = new FileWriter(tokenFile)) {
            writer.write(token);
        }
        logger.debug("Token saved to local cache: {}", tokenFile.getAbsolutePath());
    }

    /**
     * Load token from local cache
     */
    public void loadTokenFromCache() throws IOException {
        File tokenFile = new File(Configuration.getInstance().getCachePath(), "token.txt");
        if (tokenFile.exists()) {
            try (FileReader reader = new FileReader(tokenFile)) {
                StringBuilder tokenBuilder = new StringBuilder();
                int c;
                while ((c = reader.read()) != -1) {
                    tokenBuilder.append((char) c);
                }
                this.authToken = tokenBuilder.toString().trim();
                logger.debug("Token loaded from local cache");
            }
        }
    }

    /**
     * MD5 encryption for password
     */
    private String md5Encrypt(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(password.getBytes());
        byte[] bytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Get task list with pagination.
     */
    public TaskListResult getTaskList(String userId, int page, int pageSize) throws IOException {
        logger.debug("Starting getTaskList request for user ID: {}", userId);
        logger.debug("Pagination: page={}, pageSize={}", page, pageSize);
        
        CacheManager cacheManager = new CacheManager();

        String url = apiBaseUrl + TASK_LIST_ENDPOINT + "?user_id=" + userId + "&page=" + page + "&limit="
                + pageSize;
        logger.debug("Task list URL: {}", url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "JWT " + authToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            logger.debug("Task list response status: {}", response.code());

            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                logger.debug("Task list response received {}", responseBody);

                List<Task> tasks = new ArrayList<>();
                int total = 0;

                try {
                    // Try to parse as JsonObject (standard API response format)
                    JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);

                    // Check if API returned an error
                    if (responseJson.has("code")) {
                        int code = responseJson.get("code").getAsInt();
                        if (code != 2000) { // API uses 2000 for success
                            String msg = responseJson.has("msg") ? responseJson.get("msg").getAsString()
                                    : "Unknown error";
                            logger.error("API returned error: code={}, msg={}", code, msg);
                            throw new IOException("API error: " + msg);
                        }
                    }

                    // Extract total if available
                    if (responseJson.has("total") && !responseJson.get("total").isJsonNull()) {
                        total = responseJson.get("total").getAsInt();
                        logger.debug("Found total in response: {}", total);
                    }

                    // Extract data
                    if (responseJson.has("data") && !responseJson.get("data").isJsonNull()) {
                        JsonArray tasksArray = responseJson.getAsJsonArray("data");
                        logger.debug("Parsed task list from 'data' field, found {} tasks", tasksArray.size());

                        for (int i = 0; i < tasksArray.size(); i++) {
                            JsonObject taskJson = tasksArray.get(i).getAsJsonObject();
                            Task task = Task.fromJson(taskJson, cacheManager);
                            tasks.add(task);
                        }
                    } else {
                        // If no data field, try to parse as JsonArray
                        try {
                            JsonArray tasksArray = gson.fromJson(responseBody, JsonArray.class);
                            logger.debug("Parsed task list as JsonArray, found {} tasks", tasksArray.size());

                            for (int i = 0; i < tasksArray.size(); i++) {
                                JsonObject taskJson = tasksArray.get(i).getAsJsonObject();
                                Task task = Task.fromJson(taskJson, cacheManager);
                                tasks.add(task);
                            }
                            total = tasks.size();
                        } catch (JsonSyntaxException ex) {
                            logger.debug("Task list 'data' field is null or missing, and response is not a JsonArray");
                        }
                    }
                } catch (JsonSyntaxException e) {
                    logger.error("Failed to parse task list response: {}", e.getMessage());
                    throw new IOException("Failed to parse task list response: " + e.getMessage());
                }

                // Note: Cache management is now handled by the caller (UI layer)
                // ApiClient only returns data from API without caching

                logger.debug("Task list request completed, returned {} tasks with total: {}", tasks.size(), total);
                return new TaskListResult(tasks, total);
            } else {
                logger.error("Failed to get task list: {}", response.message());
                throw new IOException("Failed to get task list: " + response.message());
            }
        }
    }

    /**
     * Get WSI list for a task with pagination.
     */
    public WsiListResult getTaskWsiList(String taskId, int page, int pageSize) throws IOException {
        logger.debug("Starting getTaskWsiList request for task ID: {}", taskId);
        logger.debug("Pagination: page={}, pageSize={}", page, pageSize);
        
        CacheManager cacheManager = new CacheManager();

        String url = apiBaseUrl + TASK_WSI_LIST_ENDPOINT + "?project_task=" + taskId + "&page=" + page + "&limit="
                + pageSize;
        logger.debug("WSI list URL: {}", url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "JWT " + authToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            logger.debug("WSI list response status: {}", response.code());

            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                logger.debug("WSI list response received, parsing data");

                List<TaskFile> taskFiles = new ArrayList<>();
                int total = 0;

                try {
                    // Try to parse as JsonObject (standard API response format)
                    JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);

                    // Check if API returned an error
                    if (responseJson.has("code")) {
                        int code = responseJson.get("code").getAsInt();
                        if (code != 2000) { // API uses 2000 for success
                            String msg = responseJson.has("msg") ? responseJson.get("msg").getAsString()
                                    : "Unknown error";
                            logger.error("API returned error: code={}, msg={}", code, msg);
                            throw new IOException("API error: " + msg);
                        }
                    }

                    // Extract total if available
                    if (responseJson.has("total") && !responseJson.get("total").isJsonNull()) {
                        total = responseJson.get("total").getAsInt();
                        logger.debug("Found total in response: {}", total);
                    }

                    // Extract data
                    if (responseJson.has("data") && !responseJson.get("data").isJsonNull()) {
                        JsonArray wsiArray = responseJson.getAsJsonArray("data");
                        logger.debug("Parsed WSI list from 'data' field, found {} WSIs", wsiArray.size());

                        for (int i = 0; i < wsiArray.size(); i++) {
                            JsonObject wsiJson = wsiArray.get(i).getAsJsonObject();
                            TaskFile taskFile = TaskFile.fromJson(wsiJson, null, cacheManager);
                            taskFiles.add(taskFile);
                        }
                    } else {
                        // If no data field, try to parse as JsonArray
                        try {
                            JsonArray wsiArray = gson.fromJson(responseBody, JsonArray.class);
                            logger.debug("Parsed WSI list as JsonArray, found {} WSIs", wsiArray.size());

                            for (int i = 0; i < wsiArray.size(); i++) {
                                JsonObject wsiJson = wsiArray.get(i).getAsJsonObject();
                                TaskFile taskFile = TaskFile.fromJson(wsiJson, null, cacheManager);
                                taskFiles.add(taskFile);
                            }
                            total = taskFiles.size();
                        } catch (JsonSyntaxException ex) {
                            logger.debug("WSI list 'data' field is null or missing, and response is not a JsonArray");
                        }
                    }
                } catch (JsonSyntaxException e) {
                    logger.error("Failed to parse WSI list response: {}", e.getMessage());
                    throw new IOException("Failed to parse WSI list response: " + e.getMessage());
                }

                // Note: Cache management is now handled by the caller (UI layer)
                // ApiClient only returns data from API without caching
                // This ensures local status (local_status, local_path) can be properly merged before saving to cache

                logger.debug("WSI list request completed, returned {} WSIs with total: {}", taskFiles.size(), total);
                return new WsiListResult(taskFiles, total);
            } else {
                logger.error("Failed to get WSI list: {}", response.message());
                throw new IOException("Failed to get WSI list: " + response.message());
            }
        }
    }

    /**
     * Submit annotation for a WSI.
     */
    public boolean submitAnnotation(int wsiId, Map<String, Object> annotationData) throws IOException {
        logger.info("========== Submit Annotation ==========");
        logger.info("Starting submitAnnotation request for WSI ID: {}", wsiId);
        logger.info("Annotation data: {}", gson.toJson(annotationData));

        // Wrap annotation data in an object with "annotation" key
        java.util.Map<String, Object> wrapper = new java.util.HashMap<>();
        wrapper.put("annotation", annotationData);
        String json = gson.toJson(wrapper);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        String url = apiBaseUrl + SUBMIT_ANNOTATION_ENDPOINT.replace("{id}", String.valueOf(wsiId));
        logger.info("PUT: {}", url);

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .addHeader("Authorization", "JWT " + authToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            logger.info("Response: {} {}", response.code(), response.message());
            
            // Handle redirects to avoid PUT body loss
            if (response.code() >= 300 && response.code() < 400) {
                String location = response.header("Location");
                if (location != null) {
                    logger.warn("Received redirect {} to {}", response.code(), location);
                    logger.warn("This may cause PUT request body to be lost!");
                    
                    // Handle relative URLs
                    if (!location.startsWith("http")) {
                        // Create a new URL by resolving the relative path
                        try {
                            java.net.URL originalUrl = new java.net.URL(url);
                            java.net.URL redirectUrl = new java.net.URL(originalUrl, location);
                            location = redirectUrl.toString();
                        } catch (Exception e) {
                            logger.error("Error resolving redirect URL: {}", e.getMessage());
                        }
                    }
                    
                    logger.info("Manually following redirect to: {}", location);
                    
                    // Create a new request for the redirect URL
                    Request redirectRequest = new Request.Builder()
                            .url(location)
                            .put(body)
                            .addHeader("Authorization", "JWT " + authToken)
                            .build();
                    
                    // Execute the redirect request
                    try (Response redirectResponse = client.newCall(redirectRequest).execute()) {
                        logger.info("Redirect response: {} {}", redirectResponse.code(), redirectResponse.message());
                        boolean success = redirectResponse.isSuccessful();
                        logger.info("Submit annotation {}", success ? "successful" : "failed");
                        logger.info("=======================================");
                        return success;
                    }
                }
            }
            
            boolean success = response.isSuccessful();
            logger.info("Submit annotation {}", success ? "successful" : "failed");
            logger.info("=======================================");
            return success;
        }
    }

    /**
     * Download WSI file and save to local.
     */
    public boolean downloadWsi(String wsiUrl, String savePath) throws IOException {
        logger.debug("Starting downloadWsi request");
        logger.debug("WSI download URL: {}", wsiUrl);
        logger.debug("Save path: {}", savePath);

        // Validate and handle URL (ensure it has http/https protocol)
        if (wsiUrl == null || wsiUrl.isEmpty()) {
            throw new IOException("WSI download URL is null or empty");
        }
        
        // Check if URL has protocol prefix
        if (!wsiUrl.startsWith("http://") && !wsiUrl.startsWith("https://")) {
            logger.warn("WSI download URL missing protocol prefix, adding http://");
            wsiUrl = "http://" + wsiUrl;
        }

        Request request = new Request.Builder()
                .url(wsiUrl)
                .addHeader("Authorization", "JWT " + authToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            logger.debug("WSI download response status: {}", response.code());

            if (response.isSuccessful()) {
                // Ensure save directory exists
                File saveDir = new File(savePath).getParentFile();
                if (!saveDir.exists()) {
                    logger.debug("Creating save directory: {}", saveDir.getAbsolutePath());
                    saveDir.mkdirs();
                }

                // Save file
                try (InputStream inputStream = response.body().byteStream();
                        FileOutputStream outputStream = new FileOutputStream(savePath)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                logger.debug("WSI download successful, saved to: {}", savePath);
                return true;
            } else {
                logger.error("Failed to download WSI: {}", response.message());
                throw new IOException("Failed to download WSI: " + response.message());
            }
        }
    }

    /**
     * 下载进度回调接口
     */
    public interface DownloadProgressListener {
        void onProgress(long bytesRead, long totalBytes);
    }

    /**
     * 带进度回调的WSI下载方法
     */
    public boolean downloadWsiWithProgress(String wsiUrl, String savePath, DownloadProgressListener listener) throws IOException {
        logger.debug("Starting downloadWsiWithProgress request");
        logger.debug("WSI download URL: {}", wsiUrl);
        logger.debug("Save path: {}", savePath);

        if (wsiUrl == null || wsiUrl.isEmpty()) {
            throw new IOException("WSI download URL is null or empty");
        }

        if (!wsiUrl.startsWith("http://") && !wsiUrl.startsWith("https://")) {
            logger.warn("WSI download URL missing protocol prefix, adding http://");
            wsiUrl = "http://" + wsiUrl;
        }

        Request request = new Request.Builder()
                .url(wsiUrl)
                .addHeader("Authorization", "JWT " + authToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            logger.debug("WSI download response status: {}", response.code());

            if (response.isSuccessful()) {
                File saveDir = new File(savePath).getParentFile();
                if (!saveDir.exists()) {
                    saveDir.mkdirs();
                }

                long contentLength = response.body().contentLength();
                try (InputStream inputStream = response.body().byteStream();
                        FileOutputStream outputStream = new FileOutputStream(savePath)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        if (listener != null) {
                            listener.onProgress(totalBytesRead, contentLength);
                        }
                    }
                }

                logger.debug("WSI download successful, saved to: {}", savePath);
                return true;
            } else {
                logger.error("Failed to download WSI: {}", response.message());
                throw new IOException("Failed to download WSI: " + response.message());
            }
        }
    }

    /**
     * Submit task completion status.
     */
    public boolean submitTaskCompletion(String taskId, int status) throws IOException {
        logger.debug("Starting submitTaskCompletion request for task ID: {}", taskId);
        logger.debug("Task status to submit: {}", status);

        Map<String, Integer> statusData = Map.of(
                "status", status);

        String json = gson.toJson(statusData);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        String url = apiBaseUrl + TASK_LIST_ENDPOINT + taskId + "/";
        logger.debug("Submit task status URL: {}", url);

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .addHeader("Authorization", "JWT " + authToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            logger.debug("Submit task status response status: {}", response.code());
            boolean success = response.isSuccessful();
            logger.debug("Submit task status {}", success ? "successful" : "failed");
            return success;
        }
    }

    /**
     * Get authentication token.
     */
    public String getAuthToken() {
        return authToken;
    }

    /**
     * Set authentication token.
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
    
    /**
     * Get API base URL.
     */
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }
    
    /**
     * Set API base URL.
     */
    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
        // 同时更新配置中的值
        Configuration.getInstance().setApiBaseUrl(apiBaseUrl);
        try {
            Configuration.getInstance().save();
            logger.debug("API base URL updated and saved to configuration: {}", apiBaseUrl);
        } catch (IOException e) {
            logger.error("Failed to save API base URL to configuration: {}", e.getMessage());
        }
    }
}
