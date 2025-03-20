package viettel.dac.promptservice.model.enums;

/**
 * Status of a prompt version
 */
public enum VersionStatus {
    DRAFT("Work in progress version"),
    REVIEW("Version under review"),
    APPROVED("Approved but not published"),
    PUBLISHED("Active published version"),
    DEPRECATED("Version that should no longer be used"),
    ARCHIVED("Old version that is archived for reference");

    private final String description;

    VersionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}