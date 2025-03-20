package viettel.dac.promptservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import viettel.dac.promptservice.model.entity.PromptParameter;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersionComparisonResult {
    private String versionId1;
    private String versionNumber1;
    private String versionId2;
    private String versionNumber2;

    // Content differences
    private String originalContent;
    private String modifiedContent;
    private List<TextDiff> contentDiffs;

    // Parameter differences
    private List<PromptParameter> addedParameters;
    private List<PromptParameter> removedParameters;
    private List<ParameterDiff> modifiedParameters;

    @Data
    @Builder
    public static class TextDiff {
        private DiffType type;
        private String text;
        private int position;
    }

    @Data
    @Builder
    public static class ParameterDiff {
        private String parameterName;
        private Map<String, Object> originalValues;
        private Map<String, Object> newValues;
    }

    public enum DiffType {
        ADDITION, DELETION, UNCHANGED
    }
}