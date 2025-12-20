package com.jdepanalyzer.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for GET /api/export/{table}.csv endpoint.
 * 
 * BDD-style tests for table CSV export functionality.
 */
class ExportApiTest extends BaseApiTest {

    // ========================================================================
    // Scenario: Export artifacts table as CSV
    // ========================================================================
    @Test
    @DisplayName("Given artifacts exist, when I export artifact table, then I get CSV with artifacts")
    void exportArtifacts_shouldReturnCsv() throws IOException {
        // Given: Some artifacts in the database
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I export the artifacts table
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/export/artifacts.csv"), String.class);

        // Then: I should get a CSV file
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // And: The CSV should have correct header
        assertThat(response.getBody()).contains("gav,group_id,artifact_id,version");

        // And: The CSV should contain the exported artifacts
        assertThat(response.getBody()).contains("org.springframework:spring-core:6.2.15");
    }

    @Test
    @DisplayName("Given an empty database, when I export artifact table, then I get only header")
    void exportArtifacts_whenEmpty_shouldReturnHeaderOnly() {
        // Given: An empty database
        assertThat(artifactRepository.count()).isZero();

        // When: I export the artifacts table
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/export/artifacts.csv"), String.class);

        // Then: I should get only the header row
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String[] lines = response.getBody().trim().split("\r?\n");
        assertThat(lines).hasSize(1);
        assertThat(lines[0].trim()).isEqualTo("gav,group_id,artifact_id,version");
    }

    // ========================================================================
    // Scenario: Export edges (dependencies) table as CSV
    // ========================================================================
    @Test
    @DisplayName("Given edges exist, when I export edges table, then I get CSV with edges")
    void exportEdges_shouldReturnCsv() throws IOException {
        // Given: Some dependencies in the database
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I export the edges table
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/export/edges.csv"), String.class);

        // Then: I should get a CSV file
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // And: The CSV should have correct header
        assertThat(response.getBody()).contains("id,from_gav,to_gav,scope,optional");

        // And: The CSV should contain the edges
        assertThat(response.getBody()).contains("org.springframework:spring-core:6.2.15");
        assertThat(response.getBody()).contains("org.springframework:spring-jcl:6.2.15");
    }

    @Test
    @DisplayName("Given an empty database, when I export edges table, then I get only header")
    void exportEdges_whenEmpty_shouldReturnHeaderOnly() {
        // Given: An empty database
        assertThat(edgeRepository.count()).isZero();

        // When: I export the edges table
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/export/edges.csv"), String.class);

        // Then: I should get only the header row
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String[] lines = response.getBody().trim().split("\r?\n");
        assertThat(lines).hasSize(1);
        assertThat(lines[0].trim()).isEqualTo("id,from_gav,to_gav,scope,optional");
    }

    // ========================================================================
    // Scenario: Export with alternative table names
    // ========================================================================
    @Test
    @DisplayName("Export endpoint should accept 'artifact' as table name")
    void exportArtifact_singularName_shouldWork() throws IOException {
        // Given: Some artifacts
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I export using singular 'artifact'
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/export/artifact.csv"), String.class);

        // Then: It should work the same as 'artifacts'
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("gav,group_id,artifact_id,version");
    }

    @Test
    @DisplayName("Export endpoint should accept 'dependencies' as table name")
    void exportDependencies_alternativeName_shouldWork() throws IOException {
        // Given: Some edges
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I export using 'dependencies'
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/export/dependencies.csv"), String.class);

        // Then: It should work the same as 'edges'
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("id,from_gav,to_gav,scope,optional");
    }

    // ========================================================================
    // Scenario: Export with invalid table name
    // ========================================================================
    @Test
    @DisplayName("Given an invalid table name, when I export, then I should get 404")
    void exportInvalidTable_shouldReturn404() {
        // When: I try to export a non-existent table
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/export/nonexistent.csv"), String.class);

        // Then: I should get a 404 error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Given an invalid table name format, when I export, then I should get an error")
    void exportInvalidTableFormat_shouldReturnError() {
        // When: I try to export with invalid characters in table name
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/export/invalid;table.csv"), String.class);

        // Then: I should get an error (400 or 404 depending on how Spring parses the
        // URL)
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    // ========================================================================
    // Scenario: Export with correct content type and disposition
    // ========================================================================
    @Test
    @DisplayName("Export should set correct Content-Type and Content-Disposition headers")
    void exportArtifacts_shouldSetCorrectHeaders() throws IOException {
        // Given: Some artifacts
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I export the artifacts table
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl("/api/export/artifacts.csv"), String.class);

        // Then: Headers should be correct for CSV download
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("text/csv");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("attachment");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("artifacts.csv");
    }
}
