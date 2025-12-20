package com.jdepanalyzer.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for dependencies endpoints:
 * - GET /api/dependencies/table (JSON)
 * - GET /api/dependencies/export (CSV)
 * 
 * BDD-style tests for dependency data retrieval and export.
 */
class DependenciesApiTest extends BaseApiTest {

    // ========================================================================
    // /api/dependencies/table - JSON Table Tests
    // ========================================================================

    @Test
    @DisplayName("Given an empty database, when I request dependencies table, then I should get empty array")
    @SuppressWarnings("unchecked")
    void getDependenciesTable_whenEmpty_shouldReturnEmptyArray() {
        // Given: An empty database
        assertThat(edgeRepository.count()).isZero();

        // When: I request the dependencies table
        ResponseEntity<List> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/table"), List.class);

        // Then: I should get an empty array
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Given dependencies exist, when I request table, then I should get JSON array with data")
    @SuppressWarnings("unchecked")
    void getDependenciesTable_withData_shouldReturnJsonArray() throws IOException {
        // Given: Some dependencies in the database
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request the dependencies table
        ResponseEntity<List> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/table"), List.class);

        // Then: I should get a JSON array
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();

        // And: The array should contain dependency data
        Map<String, Object> firstRow = (Map<String, Object>) response.getBody().get(0);
        assertThat(firstRow).containsKey("fromArtifact");
        assertThat(firstRow).containsKey("toArtifact");
        assertThat(firstRow.get("fromArtifact").toString()).isEqualTo("spring-core");
        assertThat(firstRow.get("toArtifact").toString()).isEqualTo("spring-jcl");
    }

    @Test
    @DisplayName("Given dependencies exist, when I filter by query, then only matching rows appear")
    @SuppressWarnings("unchecked")
    void getDependenciesTable_withQuery_shouldFilter() throws IOException {
        // Given: Dependencies from multiple POMs
        uploadPomFiles("spring-core-6.2.15.pom", "spring-context-6.2.15.pom");

        // When: I request with a query filter
        ResponseEntity<List> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/table?q=spring-jcl"), List.class);

        // Then: Only matching artifacts should appear
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
        
        // Verify each row contains spring-jcl in either fromArtifact or toArtifact
        for (Object item : response.getBody()) {
            Map<String, Object> row = (Map<String, Object>) item;
            boolean matches = row.get("fromArtifact").toString().contains("spring-jcl") ||
                            row.get("toArtifact").toString().contains("spring-jcl");
            assertThat(matches).isTrue();
        }
    }

    @Test
    @DisplayName("Given dependencies exist, when I filter by group_q, then only matching groups appear")
    @SuppressWarnings("unchecked")
    void getDependenciesTable_withGroupQuery_shouldFilter() throws IOException {
        // Given: Dependencies from multiple POMs
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request with a group query filter
        ResponseEntity<List> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/table?group_q=springframework"), List.class);

        // Then: Only springframework group artifacts should appear
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();

        // Verify each row contains springframework in either fromGroup or toGroup
        for (Object item : response.getBody()) {
            Map<String, Object> row = (Map<String, Object>) item;
            boolean matches = row.get("fromGroup").toString().contains("springframework") ||
                            row.get("toGroup").toString().contains("springframework");
            assertThat(matches).isTrue();
        }
    }

    @Test
    @DisplayName("Given dependencies exist, when I request with ignore_version, then versions should be empty")
    @SuppressWarnings("unchecked")
    void getDependenciesTable_withIgnoreVersion_shouldHideVersions() throws IOException {
        // Given: Dependencies in the database
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request with ignore_version=true
        ResponseEntity<List> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/table?ignore_version=true"), List.class);

        // Then: Response should still be valid JSON
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();

        // And: Version fields should be empty strings
        Map<String, Object> firstRow = (Map<String, Object>) response.getBody().get(0);
        assertThat(firstRow.get("fromVersion").toString()).isEmpty();
        assertThat(firstRow.get("toVersion").toString()).isEmpty();
    }

    @Test
    @DisplayName("Given dependencies exist, when I request with limit, then only limited rows appear")
    @SuppressWarnings("unchecked")
    void getDependenciesTable_withLimit_shouldRespectLimit() throws IOException {
        // Given: Multiple dependencies from multiple POMs
        uploadPomFiles("spring-core-6.2.15.pom", "spring-context-6.2.15.pom");

        // When: I request with limit=1
        ResponseEntity<List> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/table?limit=1"), List.class);

        // Then: Only 1 row should appear
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    // ========================================================================
    // /api/dependencies/export - CSV Export Tests
    // ========================================================================

    @Test
    @DisplayName("Given an empty database, when I export dependencies, then I get only header row")
    void exportDependencies_whenEmpty_shouldReturnHeaderOnly() {
        // Given: An empty database
        assertThat(edgeRepository.count()).isZero();

        // When: I export dependencies
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/export"), String.class);

        // Then: I should get only the header row
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(
                "source_group,source_artifact,source_version,target_group,target_artifact,target_version,scope");

        // And: No data rows (only header)
        String[] lines = response.getBody().trim().split("\\r?\\n");
        assertThat(lines).hasSize(1);
    }

    @Test
    @DisplayName("Given dependencies exist, when I export, then I get CSV with data")
    void exportDependencies_withData_shouldReturnCsv() throws IOException {
        // Given: Some dependencies
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I export dependencies
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/export"), String.class);

        // Then: I should get CSV with header and data rows
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String[] lines = response.getBody().trim().split("\\r?\\n");
        assertThat(lines.length).isGreaterThan(1);

        // And: Data should include spring artifacts
        assertThat(response.getBody()).contains("org.springframework");
        assertThat(response.getBody()).contains("spring-core");
    }

    @Test
    @DisplayName("Given dependencies exist, when I export with filter, then only matching rows are exported")
    void exportDependencies_withFilter_shouldFilterResults() throws IOException {
        // Given: Dependencies from multiple POMs
        uploadPomFiles("spring-core-6.2.15.pom", "spring-context-6.2.15.pom");

        // When: I export with a query filter
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/export?q=spring-jcl"), String.class);

        // Then: Only matching rows should appear
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("spring-jcl");
    }

    @Test
    @DisplayName("CSV export should match Python version format")
    void exportDependencies_shouldMatchPythonFormat() throws IOException {
        // Given: Some dependencies
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I export dependencies
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/export"), String.class);

        // Then: Header should match Python version exactly
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String header = response.getBody().trim().split("\\r?\\n")[0].trim();
        assertThat(header).isEqualTo(
                "source_group,source_artifact,source_version,target_group,target_artifact,target_version,scope");

        // And: Data rows should have 7 columns
        String[] lines = response.getBody().trim().split("\\r?\\n");
        if (lines.length > 1) {
            String dataRow = lines[1].trim();
            String[] columns = dataRow.split(",");
            assertThat(columns).hasSize(7);
        }
    }
}

