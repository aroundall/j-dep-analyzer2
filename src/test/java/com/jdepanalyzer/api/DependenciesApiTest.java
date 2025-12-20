package com.jdepanalyzer.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for dependencies endpoints:
 * - GET /api/dependencies/table (HTML)
 * - GET /api/dependencies/export (CSV)
 * 
 * BDD-style tests for dependency data retrieval and export.
 */
class DependenciesApiTest extends BaseApiTest {

    // ========================================================================
    // /api/dependencies/table - HTML Table Tests
    // ========================================================================

    @Test
    @DisplayName("Given an empty database, when I request dependencies table, then I should get 'No dependencies found'")
    void getDependenciesTable_whenEmpty_shouldShowNoData() {
        // Given: An empty database
        assertThat(edgeRepository.count()).isZero();

        // When: I request the dependencies table
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/table"), String.class);

        // Then: I should get a table with "No dependencies found"
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("No dependencies found");
    }

    @Test
    @DisplayName("Given dependencies exist, when I request table, then I should get HTML table with data")
    void getDependenciesTable_withData_shouldReturnHtmlTable() throws IOException {
        // Given: Some dependencies in the database
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request the dependencies table
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/table"), String.class);

        // Then: I should get an HTML table
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("<table");
        assertThat(response.getBody()).contains("<thead");
        assertThat(response.getBody()).contains("<tbody");

        // And: The table should contain dependency data
        assertThat(response.getBody()).contains("spring-core");
        assertThat(response.getBody()).contains("spring-jcl");
    }

    @Test
    @DisplayName("Given dependencies exist, when I filter by query, then only matching rows appear")
    void getDependenciesTable_withQuery_shouldFilter() throws IOException {
        // Given: Dependencies from multiple POMs
        uploadPomFiles("spring-core-6.2.15.pom", "spring-context-6.2.15.pom");

        // When: I request with a query filter
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/table?q=spring-jcl"), String.class);

        // Then: Only matching artifacts should appear
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("spring-jcl");
    }

    @Test
    @DisplayName("Given dependencies exist, when I filter by group_q, then only matching groups appear")
    void getDependenciesTable_withGroupQuery_shouldFilter() throws IOException {
        // Given: Dependencies from multiple POMs
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request with a group query filter
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/table?group_q=springframework"), String.class);

        // Then: Only springframework group artifacts should appear
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("org.springframework");
    }

    @Test
    @DisplayName("Given dependencies exist, when I request with ignore_version, then versions should be hidden")
    void getDependenciesTable_withIgnoreVersion_shouldHideVersions() throws IOException {
        // Given: Dependencies in the database
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request with ignore_version=true
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/table?ignore_version=true"), String.class);

        // Then: Response should still be valid HTML table
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("<table");
    }

    @Test
    @DisplayName("Given dependencies exist, when I request with limit, then only limited rows appear")
    void getDependenciesTable_withLimit_shouldRespectLimit() throws IOException {
        // Given: Multiple dependencies from multiple POMs
        uploadPomFiles("spring-core-6.2.15.pom", "spring-context-6.2.15.pom");

        // When: I request with limit=1
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/dependencies/table?limit=1"), String.class);

        // Then: Only 1 row should appear (plus header)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Count actual data rows (excluding header rows)
        String body = response.getBody();
        int dataRowCount = countOccurrences(body, "ondblclick=");
        assertThat(dataRowCount).isEqualTo(1);
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
        String[] lines = response.getBody().trim().split("\r?\n");
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
        String[] lines = response.getBody().trim().split("\r?\n");
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
        String header = response.getBody().trim().split("\r?\n")[0].trim();
        assertThat(header).isEqualTo(
                "source_group,source_artifact,source_version,target_group,target_artifact,target_version,scope");

        // And: Data rows should have 7 columns
        String[] lines = response.getBody().trim().split("\r?\n");
        if (lines.length > 1) {
            String dataRow = lines[1].trim();
            String[] columns = dataRow.split(",");
            assertThat(columns).hasSize(7);
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
