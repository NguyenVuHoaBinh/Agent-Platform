package viettel.dac.promptservice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import viettel.dac.promptservice.dto.response.VersionComparisonResult.TextDiff;
import viettel.dac.promptservice.dto.response.VersionComparisonResult.DiffType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DiffUtilityTest {

    private DiffUtility diffUtility;

    @BeforeEach
    public void setUp() {
        diffUtility = new DiffUtility();
    }

    @Test
    public void testGenerateTextDiff_NullInputs() {
        // Test with null original
        List<TextDiff> diffs = diffUtility.generateTextDiff(null, "modified");
        assertNotNull(diffs);
        assertEquals(8, diffs.size());  // "modified" has 8 characters

        // Test with null modified
        diffs = diffUtility.generateTextDiff("original", null);
        assertNotNull(diffs);
        assertEquals(8, diffs.size());  // "original" has 8 characters

        // Test with both null
        diffs = diffUtility.generateTextDiff(null, null);
        assertNotNull(diffs);
        assertEquals(0, diffs.size());
    }

    @Test
    public void testGenerateTextDiff_EmptyStrings() {
        // Test with empty original
        List<TextDiff> diffs = diffUtility.generateTextDiff("", "modified");
        assertNotNull(diffs);
        assertEquals(8, diffs.size());  // "modified" has 8 characters
        for (TextDiff diff : diffs) {
            assertEquals(DiffType.ADDITION, diff.getType());
        }

        // Test with empty modified
        diffs = diffUtility.generateTextDiff("original", "");
        assertNotNull(diffs);
        assertEquals(8, diffs.size());  // "original" has 8 characters
        for (TextDiff diff : diffs) {
            assertEquals(DiffType.DELETION, diff.getType());
        }

        // Test with both empty
        diffs = diffUtility.generateTextDiff("", "");
        assertNotNull(diffs);
        assertEquals(0, diffs.size());
    }

    @Test
    public void testGenerateTextDiff_IdenticalStrings() {
        String text = "identical text";
        List<TextDiff> diffs = diffUtility.generateTextDiff(text, text);
        assertNotNull(diffs);
        assertEquals(text.length(), diffs.size());
        for (TextDiff diff : diffs) {
            assertEquals(DiffType.UNCHANGED, diff.getType());
        }
    }

    @Test
    public void testGenerateTextDiff_SingleCharacterAddition() {
        List<TextDiff> diffs = diffUtility.generateTextDiff("abc", "abcX");
        assertNotNull(diffs);

        // Expected: 3 unchanged characters ('a', 'b', 'c') and 1 addition ('X')
        assertEquals(4, diffs.size());

        for (int i = 0; i < 3; i++) {
            assertEquals(DiffType.UNCHANGED, diffs.get(i).getType());
        }
        assertEquals(DiffType.ADDITION, diffs.get(3).getType());
        assertEquals("X", diffs.get(3).getText());
    }

    @Test
    public void testGenerateTextDiff_SingleCharacterDeletion() {
        List<TextDiff> diffs = diffUtility.generateTextDiff("abcX", "abc");
        assertNotNull(diffs);

        // Expected: 3 unchanged characters ('a', 'b', 'c') and 1 deletion ('X')
        assertEquals(4, diffs.size());

        for (int i = 0; i < 3; i++) {
            assertEquals(DiffType.UNCHANGED, diffs.get(i).getType());
        }
        assertEquals(DiffType.DELETION, diffs.get(3).getType());
        assertEquals("X", diffs.get(3).getText());
    }

    @Test
    public void testGenerateTextDiff_SingleCharacterModification() {
        List<TextDiff> diffs = diffUtility.generateTextDiff("abcX", "abcY");
        assertNotNull(diffs);

        // Expected: 3 unchanged characters ('a', 'b', 'c'), 1 deletion ('X'), and 1 addition ('Y')
        assertEquals(5, diffs.size());

        for (int i = 0; i < 3; i++) {
            assertEquals(DiffType.UNCHANGED, diffs.get(i).getType());
        }

        // The order might be different depending on implementation
        boolean hasAddition = false;
        boolean hasDeletion = false;

        for (int i = 3; i < 5; i++) {
            if (diffs.get(i).getType() == DiffType.ADDITION) {
                hasAddition = true;
                assertEquals("Y", diffs.get(i).getText());
            } else if (diffs.get(i).getType() == DiffType.DELETION) {
                hasDeletion = true;
                assertEquals("X", diffs.get(i).getText());
            }
        }

        assertTrue(hasAddition && hasDeletion);
    }

    @Test
    public void testGenerateTextDiff_MultipleCharactersAddition() {
        List<TextDiff> diffs = diffUtility.generateTextDiff("abc", "abcXYZ");
        assertNotNull(diffs);

        // Expected: 3 unchanged characters ('a', 'b', 'c') and 3 additions ('X', 'Y', 'Z')
        assertEquals(6, diffs.size());

        for (int i = 0; i < 3; i++) {
            assertEquals(DiffType.UNCHANGED, diffs.get(i).getType());
        }

        for (int i = 3; i < 6; i++) {
            assertEquals(DiffType.ADDITION, diffs.get(i).getType());
        }

        assertEquals("X", diffs.get(3).getText());
        assertEquals("Y", diffs.get(4).getText());
        assertEquals("Z", diffs.get(5).getText());
    }

    @Test
    public void testGenerateTextDiff_MultipleCharactersDeletion() {
        List<TextDiff> diffs = diffUtility.generateTextDiff("abcXYZ", "abc");
        assertNotNull(diffs);

        // Expected: 3 unchanged characters ('a', 'b', 'c') and 3 deletions ('X', 'Y', 'Z')
        assertEquals(6, diffs.size());

        for (int i = 0; i < 3; i++) {
            assertEquals(DiffType.UNCHANGED, diffs.get(i).getType());
        }

        for (int i = 3; i < 6; i++) {
            assertEquals(DiffType.DELETION, diffs.get(i).getType());
        }

        assertEquals("X", diffs.get(3).getText());
        assertEquals("Y", diffs.get(4).getText());
        assertEquals("Z", diffs.get(5).getText());
    }

    @Test
    public void testGenerateTextDiff_ComplexDiff() {
        String original = "The quick brown fox jumps over the lazy dog";
        String modified = "The quick red fox quickly jumps over lazy dogs";

        List<TextDiff> diffs = diffUtility.generateTextDiff(original, modified);
        assertNotNull(diffs);

        // Verify specific key differences are identified
        boolean hasBrownDeletion = false;
        boolean hasRedAddition = false;
        boolean hasQuicklyAddition = false;
        boolean hasTheDeletion = false;
        boolean hasDogsAddition = false;

        for (TextDiff diff : diffs) {
            if (diff.getType() == DiffType.DELETION && diff.getText().equals("brown")) {
                hasBrownDeletion = true;
            } else if (diff.getType() == DiffType.ADDITION && diff.getText().equals("red")) {
                hasRedAddition = true;
            } else if (diff.getType() == DiffType.ADDITION && diff.getText().equals("quickly")) {
                hasQuicklyAddition = true;
            } else if (diff.getType() == DiffType.DELETION && diff.getText().equals("the")) {
                hasTheDeletion = true;
            } else if (diff.getType() == DiffType.ADDITION && diff.getText().equals("s")) {
                hasDogsAddition = true;
            }
        }

        assertTrue(hasBrownDeletion);
        assertTrue(hasRedAddition);
        assertTrue(hasQuicklyAddition);
        assertTrue(hasTheDeletion);
        assertTrue(hasDogsAddition);
    }

    @Test
    public void testConsolidateDiffs_EmptyList() {
        List<TextDiff> diffs = diffUtility.consolidateDiffs(new ArrayList<>());
        assertNotNull(diffs);
        assertEquals(0, diffs.size());
    }

    @Test
    public void testConsolidateDiffs_SingleDiff() {
        List<TextDiff> charDiffs = new ArrayList<>();
        charDiffs.add(TextDiff.builder()
                .type(DiffType.ADDITION)
                .text("A")
                .position(0)
                .build());

        List<TextDiff> diffs = diffUtility.consolidateDiffs(charDiffs);
        assertNotNull(diffs);
        assertEquals(1, diffs.size());
        assertEquals(DiffType.ADDITION, diffs.get(0).getType());
        assertEquals("A", diffs.get(0).getText());
        assertEquals(0, diffs.get(0).getPosition());
    }

    @Test
    public void testConsolidateDiffs_AdjacentSameType() {
        List<TextDiff> charDiffs = new ArrayList<>();
        charDiffs.add(TextDiff.builder().type(DiffType.ADDITION).text("A").position(0).build());
        charDiffs.add(TextDiff.builder().type(DiffType.ADDITION).text("B").position(1).build());
        charDiffs.add(TextDiff.builder().type(DiffType.ADDITION).text("C").position(2).build());

        List<TextDiff> diffs = diffUtility.consolidateDiffs(charDiffs);
        assertNotNull(diffs);
        assertEquals(1, diffs.size());
        assertEquals(DiffType.ADDITION, diffs.get(0).getType());
        assertEquals("ABC", diffs.get(0).getText());
        assertEquals(0, diffs.get(0).getPosition());
    }

    @Test
    public void testConsolidateDiffs_AdjacentDifferentTypes() {
        List<TextDiff> charDiffs = new ArrayList<>();
        charDiffs.add(TextDiff.builder().type(DiffType.ADDITION).text("A").position(0).build());
        charDiffs.add(TextDiff.builder().type(DiffType.ADDITION).text("B").position(1).build());
        charDiffs.add(TextDiff.builder().type(DiffType.UNCHANGED).text("C").position(2).build());
        charDiffs.add(TextDiff.builder().type(DiffType.UNCHANGED).text("D").position(3).build());
        charDiffs.add(TextDiff.builder().type(DiffType.DELETION).text("E").position(4).build());

        List<TextDiff> diffs = diffUtility.consolidateDiffs(charDiffs);
        assertNotNull(diffs);
        assertEquals(3, diffs.size());

        assertEquals(DiffType.ADDITION, diffs.get(0).getType());
        assertEquals("AB", diffs.get(0).getText());
        assertEquals(0, diffs.get(0).getPosition());

        assertEquals(DiffType.UNCHANGED, diffs.get(1).getType());
        assertEquals("CD", diffs.get(1).getText());
        assertEquals(2, diffs.get(1).getPosition());

        assertEquals(DiffType.DELETION, diffs.get(2).getType());
        assertEquals("E", diffs.get(2).getText());
        assertEquals(4, diffs.get(2).getPosition());
    }

    @Test
    public void testConsolidateDiffs_ComplexMixedDiffs() {
        List<TextDiff> charDiffs = new ArrayList<>();
        // Add-Add-Add-Unchanged-Unchanged-Del-Del-Unchanged-Add-Unchanged
        charDiffs.add(TextDiff.builder().type(DiffType.ADDITION).text("A").position(0).build());
        charDiffs.add(TextDiff.builder().type(DiffType.ADDITION).text("B").position(1).build());
        charDiffs.add(TextDiff.builder().type(DiffType.ADDITION).text("C").position(2).build());
        charDiffs.add(TextDiff.builder().type(DiffType.UNCHANGED).text("D").position(3).build());
        charDiffs.add(TextDiff.builder().type(DiffType.UNCHANGED).text("E").position(4).build());
        charDiffs.add(TextDiff.builder().type(DiffType.DELETION).text("F").position(5).build());
        charDiffs.add(TextDiff.builder().type(DiffType.DELETION).text("G").position(6).build());
        charDiffs.add(TextDiff.builder().type(DiffType.UNCHANGED).text("H").position(7).build());
        charDiffs.add(TextDiff.builder().type(DiffType.ADDITION).text("I").position(8).build());
        charDiffs.add(TextDiff.builder().type(DiffType.UNCHANGED).text("J").position(9).build());

        List<TextDiff> diffs = diffUtility.consolidateDiffs(charDiffs);
        assertNotNull(diffs);
        assertEquals(6, diffs.size());

        // Verify correct consolidation
        assertEquals(DiffType.ADDITION, diffs.get(0).getType());
        assertEquals("ABC", diffs.get(0).getText());

        assertEquals(DiffType.UNCHANGED, diffs.get(1).getType());
        assertEquals("DE", diffs.get(1).getText());

        assertEquals(DiffType.DELETION, diffs.get(2).getType());
        assertEquals("FG", diffs.get(2).getText());

        assertEquals(DiffType.UNCHANGED, diffs.get(3).getType());
        assertEquals("H", diffs.get(3).getText());

        assertEquals(DiffType.ADDITION, diffs.get(4).getType());
        assertEquals("I", diffs.get(4).getText());

        assertEquals(DiffType.UNCHANGED, diffs.get(5).getType());
        assertEquals("J", diffs.get(5).getText());
    }
}