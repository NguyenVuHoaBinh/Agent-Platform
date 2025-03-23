package viettel.dac.promptservice.model.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import viettel.dac.promptservice.model.enums.ParameterType;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PromptTemplateTest {

    private PromptTemplate template;

    @BeforeEach
    void setUp() {
        template = new PromptTemplate();
        template.setId("template-1");
        template.setName("Test Template");
        template.setProjectId("project-1");
        template.setCreatedBy("user-1");
    }

    @Nested
    @DisplayName("Version Management Tests")
    class VersionManagementTests {

        @Test
        @DisplayName("Should add version to template correctly")
        void shouldAddVersionToTemplateCorrectly() {
            // Arrange
            PromptVersion version = new PromptVersion();
            version.setId("version-1");

            // Act
            template.addVersion(version);

            // Assert
            assertTrue(template.getVersions().contains(version));
            assertEquals(template, version.getTemplate());
        }

        @Test
        @DisplayName("Should remove version from template correctly")
        void shouldRemoveVersionFromTemplateCorrectly() {
            // Arrange
            PromptVersion version = new PromptVersion();
            version.setId("version-1");
            template.addVersion(version);

            // Act
            template.removeVersion(version);

            // Assert
            assertFalse(template.getVersions().contains(version));
            assertNull(version.getTemplate());
        }
    }

    @Nested
    @DisplayName("Published Version Tests")
    class PublishedVersionTests {

        @Test
        @DisplayName("Should get latest published version correctly")
        void shouldGetLatestPublishedVersionCorrectly() {
            // Arrange
            PromptVersion v1 = createVersion("1.0.0", VersionStatus.PUBLISHED, LocalDateTime.now().minusDays(3));
            PromptVersion v2 = createVersion("1.1.0", VersionStatus.PUBLISHED, LocalDateTime.now().minusDays(1));
            PromptVersion v3 = createVersion("1.2.0", VersionStatus.DRAFT, LocalDateTime.now());

            template.addVersion(v1);
            template.addVersion(v2);
            template.addVersion(v3);

            // Act
            Optional<PromptVersion> latest = template.getLatestPublishedVersion();

            // Assert
            assertTrue(latest.isPresent());
            assertEquals("1.1.0", latest.get().getVersionNumber());
        }

        @Test
        @DisplayName("Should return empty when no published versions exist")
        void shouldReturnEmptyWhenNoPublishedVersionsExist() {
            // Arrange
            PromptVersion v1 = createVersion("1.0.0", VersionStatus.DRAFT, LocalDateTime.now());
            PromptVersion v2 = createVersion("1.1.0", VersionStatus.REVIEW, LocalDateTime.now());

            template.addVersion(v1);
            template.addVersion(v2);

            // Act
            Optional<PromptVersion> latest = template.getLatestPublishedVersion();

            // Assert
            assertFalse(latest.isPresent());
        }

        @Test
        @DisplayName("Should report hasPublishedVersion correctly")
        void shouldReportHasPublishedVersionCorrectly() {
            // Arrange & Act & Assert - No versions
            assertFalse(template.hasPublishedVersion());

            // Arrange & Act & Assert - No published versions
            PromptVersion draft = createVersion("1.0.0", VersionStatus.DRAFT, LocalDateTime.now());
            template.addVersion(draft);
            assertFalse(template.hasPublishedVersion());

            // Arrange & Act & Assert - With published version
            PromptVersion published = createVersion("1.1.0", VersionStatus.PUBLISHED, LocalDateTime.now());
            template.addVersion(published);
            assertTrue(template.hasPublishedVersion());
        }
    }

    @Nested
    @DisplayName("Version Creation Tests")
    class VersionCreationTests {

        @Test
        @DisplayName("Should create draft from existing version correctly")
        void shouldCreateDraftFromExistingVersionCorrectly() {
            // Arrange
            PromptVersion sourceVersion = createVersion("1.0.0", VersionStatus.PUBLISHED, LocalDateTime.now());
            sourceVersion.setContent("Test content with {{param1}} and {{param2}}");

            // Add parameters to source version
            PromptParameter param1 = new PromptParameter();
            param1.setName("param1");
            param1.setParameterType(ParameterType.STRING);
            param1.setRequired(true);
            sourceVersion.addParameter(param1);

            PromptParameter param2 = new PromptParameter();
            param2.setName("param2");
            param2.setParameterType(ParameterType.NUMBER);
            param2.setRequired(false);
            sourceVersion.addParameter(param2);

            template.addVersion(sourceVersion);

            // Act
            PromptVersion newDraft = template.createDraftFromVersion(sourceVersion, "user-2");

            // Assert
            assertEquals(VersionStatus.DRAFT, newDraft.getStatus());
            assertEquals("user-2", newDraft.getCreatedBy());
            assertEquals(sourceVersion.getContent(), newDraft.getContent());
            assertEquals(sourceVersion, newDraft.getParentVersion());

            // Check parameters were copied
            assertEquals(2, newDraft.getParameters().size());
            assertTrue(template.getVersions().contains(newDraft));

            // Check version numbers
            assertEquals("1.1.0", newDraft.getVersionNumber());
        }
    }

    // Helper method to create versions for testing
    private PromptVersion createVersion(String versionNumber, VersionStatus status, LocalDateTime createdAt) {
        PromptVersion version = new PromptVersion();
        version.setId("version-" + versionNumber);
        version.setVersionNumber(versionNumber);
        version.setStatus(status);
        version.setCreatedAt(createdAt);
        return version;
    }
}