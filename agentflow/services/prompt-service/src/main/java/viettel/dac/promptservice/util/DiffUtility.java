package viettel.dac.promptservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import viettel.dac.promptservice.dto.response.VersionComparisonResult.TextDiff;
import viettel.dac.promptservice.dto.response.VersionComparisonResult.DiffType;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class DiffUtility {

    /**
     * Generate text differences between two strings.
     */
    public List<TextDiff> generateTextDiff(String original, String modified) {
        if (original == null) original = "";
        if (modified == null) modified = "";

        // Implementation uses longest common subsequence algorithm

        List<TextDiff> diffs = new ArrayList<>();

        // Calculate lengths
        int originalLength = original.length();
        int modifiedLength = modified.length();

        // Build LCS matrix
        int[][] lcs = new int[originalLength + 1][modifiedLength + 1];

        for (int i = 1; i <= originalLength; i++) {
            for (int j = 1; j <= modifiedLength; j++) {
                if (original.charAt(i - 1) == modified.charAt(j - 1)) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i - 1][j], lcs[i][j - 1]);
                }
            }
        }

        // Reconstruct diff from LCS matrix
        int i = originalLength;
        int j = modifiedLength;

        List<TextDiff> tempDiffs = new ArrayList<>();

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && original.charAt(i - 1) == modified.charAt(j - 1)) {
                // Character is the same
                tempDiffs.add(TextDiff.builder()
                        .type(DiffType.UNCHANGED)
                        .text(String.valueOf(original.charAt(i - 1)))
                        .position(i - 1)
                        .build());
                i--;
                j--;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                // Character added in modified
                tempDiffs.add(TextDiff.builder()
                        .type(DiffType.ADDITION)
                        .text(String.valueOf(modified.charAt(j - 1)))
                        .position(j - 1)
                        .build());
                j--;
            } else {
                // Character deleted from original
                tempDiffs.add(TextDiff.builder()
                        .type(DiffType.DELETION)
                        .text(String.valueOf(original.charAt(i - 1)))
                        .position(i - 1)
                        .build());
                i--;
            }
        }

        // Reverse the list as we built it backward
        for (int k = tempDiffs.size() - 1; k >= 0; k--) {
            diffs.add(tempDiffs.get(k));
        }

        return diffs;
    }

    /**
     * Consolidate character-by-character diffs into word or line level diffs.
     */
    public List<TextDiff> consolidateDiffs(List<TextDiff> charDiffs) {
        // Consolidates adjacent diffs of the same type for better readability

        List<TextDiff> consolidated = new ArrayList<>();

        if (charDiffs.isEmpty()) {
            return consolidated;
        }

        StringBuilder currentText = new StringBuilder();
        DiffType currentType = charDiffs.get(0).getType();
        int startPosition = charDiffs.get(0).getPosition();

        for (TextDiff diff : charDiffs) {
            if (diff.getType() == currentType) {
                currentText.append(diff.getText());
            } else {
                // Type changed, add the consolidated diff
                consolidated.add(TextDiff.builder()
                        .type(currentType)
                        .text(currentText.toString())
                        .position(startPosition)
                        .build());

                // Start a new consolidated diff
                currentText = new StringBuilder(diff.getText());
                currentType = diff.getType();
                startPosition = diff.getPosition();
            }
        }

        // Add the last consolidated diff
        consolidated.add(TextDiff.builder()
                .type(currentType)
                .text(currentText.toString())
                .position(startPosition)
                .build());

        return consolidated;
    }
}