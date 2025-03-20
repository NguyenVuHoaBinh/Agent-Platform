package viettel.dac.promptservice.model.enums;

/**
 * Type of prompt parameter
 */
public enum ParameterType {
    STRING("Text string parameter"),
    NUMBER("Numeric parameter"),
    BOOLEAN("Boolean parameter (true/false)"),
    ARRAY("Array/list of values"),
    OBJECT("JSON object parameter"),
    DATE("Date parameter"),
    DATETIME("Date and time parameter");

    private final String description;

    ParameterType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}