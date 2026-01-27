package qupath.extension.pathscope.data;

import com.google.gson.JsonObject;

/**
 * 项目类，存储项目信息
 */
public class Project {

    private String id;
    private String name;
    private String description;

    /**
     * 初始化项目对象
     */
    public Project(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    /**
     * 从JsonObject创建项目对象
     */
    public static Project fromJson(JsonObject json) {
        String id = json.has("id") && !json.get("id").isJsonNull() ? json.get("id").getAsString() : "";
        String name = json.has("name") && !json.get("name").isJsonNull() ? json.get("name").getAsString() : "";
        String description = json.has("description") && !json.get("description").isJsonNull() ? json.get("description").getAsString() : "";
        return new Project(id, name, description);
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Project{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
