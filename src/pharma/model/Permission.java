package pharma.model;

/**
 * POJO representing a discrete action Permission.
 */
public class Permission {
    private int permissionId;
    private String permissionName;
    private String module;
    private String description;

    public Permission() {}

    public Permission(int permissionId, String permissionName, String module, String description) {
        this.permissionId = permissionId;
        this.permissionName = permissionName;
        this.module = module;
        this.description = description;
    }

    public int getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(int permissionId) {
        this.permissionId = permissionId;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
