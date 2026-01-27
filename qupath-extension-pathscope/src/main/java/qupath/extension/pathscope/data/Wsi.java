package qupath.extension.pathscope.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Optional;

/**
 * WSI类，存储WSI相关信息，包括project_wsi关系表id
 */
public class Wsi {

    /**
     * WSI状态枚举
     */
    public enum Status {
        DEFAULT("default"),
        DOWNLOADING("downloading"),
        DOWNLOADED("downloaded"),
        ANNOTATED("annotated");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        /**
         * 根据字符串值获取状态枚举
         */
        public static Status fromValue(String value) {
            for (Status status : Status.values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return DEFAULT;
        }
    }

    private int projectWsiId;
    private int id;
    private String name;
    private String path;
    private long size;
    private String ptype;
    private String url;
    private String downloadUrl;
    private String description;
    private String modifier;
    private String modifierName;
    private String creator;
    private String creatorName;
    private Integer deptBelongId;
    private String createDatetime;
    private String updateDatetime;
    private Status status;

    /**
     * 初始化WSI对象
     */
    public Wsi(int projectWsiId, int id, String name, String path, long size, String ptype, String url,
            String downloadUrl,
            String description, String modifier, String modifierName, String creator, String creatorName,
            Integer deptBelongId, String createDatetime, String updateDatetime, Status status) {
        this.projectWsiId = projectWsiId;
        this.id = id;
        this.name = name;
        this.path = path;
        this.size = size;
        this.ptype = ptype;
        this.url = url;
        this.downloadUrl = downloadUrl;
        this.description = description;
        this.modifier = modifier;
        this.modifierName = modifierName;
        this.creator = creator;
        this.creatorName = creatorName;
        this.deptBelongId = deptBelongId;
        this.createDatetime = createDatetime;
        this.updateDatetime = updateDatetime;
        this.status = status != null ? status : Status.DEFAULT;
    }

    /**
     * 从JsonObject创建WSI对象
     */
    public static Wsi fromJson(JsonObject json) {
        int projectWsiId = json.has("project_wsi_id") && !json.get("project_wsi_id").isJsonNull()
                ? json.get("project_wsi_id").getAsInt()
                : 0;
        int id = json.has("id") && !json.get("id").isJsonNull() ? json.get("id").getAsInt() : 0;
        String name = json.has("name") && !json.get("name").isJsonNull() ? json.get("name").getAsString() : "";
        String path = json.has("path") && !json.get("path").isJsonNull() ? json.get("path").getAsString() : "";
        long size = json.has("size") && !json.get("size").isJsonNull() ? json.get("size").getAsLong() : 0;
        String ptype = json.has("ptype") && !json.get("ptype").isJsonNull() ? json.get("ptype").getAsString() : "";
        String url = json.has("url") && !json.get("url").isJsonNull() ? json.get("url").getAsString() : "";
        String downloadUrl = json.has("download_url") && !json.get("download_url").isJsonNull()
                ? json.get("download_url").getAsString()
                : "";
        String description = json.has("description") && !json.get("description").isJsonNull()
                ? json.get("description").getAsString()
                : "";
        String modifier = json.has("modifier") && !json.get("modifier").isJsonNull()
                ? json.get("modifier").getAsString()
                : "";
        String modifierName = json.has("modifier_name") && !json.get("modifier_name").isJsonNull()
                ? json.get("modifier_name").getAsString()
                : "";
        String creator = json.has("creator") && !json.get("creator").isJsonNull() ? json.get("creator").getAsString()
                : "";
        String creatorName = json.has("creator_name") && !json.get("creator_name").isJsonNull()
                ? json.get("creator_name").getAsString()
                : "";
        Integer deptBelongId = json.has("dept_belong_id") && !json.get("dept_belong_id").isJsonNull()
                ? json.get("dept_belong_id").getAsInt()
                : null;
        String createDatetime = json.has("create_datetime") && !json.get("create_datetime").isJsonNull()
                ? json.get("create_datetime").getAsString()
                : "";
        String updateDatetime = json.has("update_datetime") && !json.get("update_datetime").isJsonNull()
                ? json.get("update_datetime").getAsString()
                : "";
        Status status = json.has("status") && !json.get("status").isJsonNull()
                ? Status.fromValue(json.get("status").getAsString())
                : Status.DEFAULT;

        return new Wsi(projectWsiId, id, name, path, size, ptype, url, downloadUrl, description, modifier, modifierName,
                creator, creatorName, deptBelongId, createDatetime, updateDatetime, status);
    }

    /**
     * 从project_wsi_obj创建WSI对象
     */
    public static Wsi fromSrc(JsonObject json) {
        int projectWsiId = json.has("project_wsi_id") && !json.get("project_wsi_id").isJsonNull()
                ? json.get("project_wsi_id").getAsInt()
                : 0;
        JsonObject wsiObj = json.has("wsi_obj") && !json.get("wsi_obj").isJsonNull() ? json.getAsJsonObject("wsi_obj")
                : new JsonObject();
        return fromJson(wsiObj);
    }

    // Getters and setters
    public int getProjectWsiId() {
        return projectWsiId;
    }

    public void setProjectWsiId(int projectWsiId) {
        this.projectWsiId = projectWsiId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPtype() {
        return ptype;
    }

    public void setPtype(String ptype) {
        this.ptype = ptype;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public String getModifierName() {
        return modifierName;
    }

    public void setModifierName(String modifierName) {
        this.modifierName = modifierName;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public Integer getDeptBelongId() {
        return deptBelongId;
    }

    public void setDeptBelongId(Integer deptBelongId) {
        this.deptBelongId = deptBelongId;
    }

    public String getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(String createDatetime) {
        this.createDatetime = createDatetime;
    }

    public String getUpdateDatetime() {
        return updateDatetime;
    }

    public void setUpdateDatetime(String updateDatetime) {
        this.updateDatetime = updateDatetime;
    }

    /**
     * 获取WSI状态
     */
    public Status getStatus() {
        return status;
    }

    /**
     * 设置WSI状态
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Wsi{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", status='" + status.getValue() + '\'' +
                '}';
    }
}
