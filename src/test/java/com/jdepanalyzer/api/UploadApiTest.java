package com.jdepanalyzer.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for POST /api/upload endpoint.
 * 
 * BDD-style tests for POM file upload functionality.
 */
class UploadApiTest extends BaseApiTest {

    // ========================================================================
    // Scenario: Upload a valid POM file successfully
    // ========================================================================
    @Test
    @DisplayName("Given an empty database, when I upload a valid POM file, then it should succeed and store artifacts")
    void uploadValidPomFile_shouldStoreArtifactsAndEdges() throws IOException {
        // Given: An empty database
        assertThat(artifactRepository.count()).isZero();
        assertThat(edgeRepository.count()).isZero();

        // When: I upload a valid POM file (spring-core with 1 dependency)
        HttpEntity<MultiValueMap<String, Object>> entity = createUploadEntity("spring-core-6.2.15.pom");
        ResponseEntity<String> response = restTemplate.postForEntity(
                apiUrl("/api/upload"), entity, String.class);

        // Then: The response should indicate success
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Upload Complete");
        assertThat(response.getBody()).contains("Parsed: 1 files");

        // And: The database should contain the artifact and its dependencies
        assertThat(artifactRepository.count()).isEqualTo(2); // spring-core + spring-jcl
        assertThat(edgeRepository.count()).isEqualTo(1); // 1 dependency edge

        // And: The project artifact should be stored
        assertThat(artifactRepository.existsById("org.springframework:spring-core:6.2.15")).isTrue();
        // And: The dependency artifact should be stored
        assertThat(artifactRepository.existsById("org.springframework:spring-jcl:6.2.15")).isTrue();
    }

    // ========================================================================
    // Scenario: Upload multiple POM files at once
    // ========================================================================
    @Test
    @DisplayName("Given an empty database, when I upload multiple POM files, then all should be processed")
    void uploadMultiplePomFiles_shouldProcessAll() throws IOException {
        // Given: An empty database
        assertThat(artifactRepository.count()).isZero();

        // When: I upload multiple POM files
        HttpEntity<MultiValueMap<String, Object>> entity = createUploadEntity(
                "spring-core-6.2.15.pom",
                "spring-context-6.2.15.pom");
        ResponseEntity<String> response = restTemplate.postForEntity(
                apiUrl("/api/upload"), entity, String.class);

        // Then: The response should indicate success for both files
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Parsed: 2 files");

        // And: All artifacts should be stored
        assertThat(artifactRepository.count()).isGreaterThanOrEqualTo(2);
    }

    // ========================================================================
    // Scenario: Upload same POM file twice (idempotent)
    // ========================================================================
    @Test
    @DisplayName("Given an artifact already exists, when I upload the same POM, then it should not create duplicates")
    void uploadSamePomTwice_shouldBeIdempotent() throws IOException {
        // Given: I've already uploaded a POM file
        uploadPomFiles("spring-core-6.2.15.pom");
        long initialArtifactCount = artifactRepository.count();
        long initialEdgeCount = edgeRepository.count();

        // When: I upload the same POM file again
        HttpEntity<MultiValueMap<String, Object>> entity = createUploadEntity("spring-core-6.2.15.pom");
        ResponseEntity<String> response = restTemplate.postForEntity(
                apiUrl("/api/upload"), entity, String.class);

        // Then: The response should indicate success
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // And: No new artifacts or edges should be created
        assertThat(artifactRepository.count()).isEqualTo(initialArtifactCount);
        assertThat(edgeRepository.count()).isEqualTo(initialEdgeCount);
    }

    // ========================================================================
    // Scenario: Upload POM with different versions of same artifact
    // ========================================================================
    @Test
    @DisplayName("Given an artifact exists, when I upload a different version, then both versions should be stored")
    void uploadDifferentVersions_shouldStoreBoth() throws IOException {
        // Given: I've uploaded one version
        uploadPomFiles("slf4j-reload4j-2.0.15.pom");

        // When: I upload a different version of the same artifact
        uploadPomFiles("slf4j-reload4j-2.0.17.pom");

        // Then: Both versions should exist in the database
        assertThat(artifactRepository.existsById("org.slf4j:slf4j-reload4j:2.0.15")).isTrue();
        assertThat(artifactRepository.existsById("org.slf4j:slf4j-reload4j:2.0.17")).isTrue();
    }

    // ========================================================================
    // Scenario: Upload response includes correct counts
    // ========================================================================
    @Test
    @DisplayName("When I upload POMs, then the response should show correct counts")
    void uploadResponse_shouldShowCorrectCounts() throws IOException {
        // When: I upload a POM with 1 dependency
        HttpEntity<MultiValueMap<String, Object>> entity = createUploadEntity("spring-core-6.2.15.pom");
        ResponseEntity<String> response = restTemplate.postForEntity(
                apiUrl("/api/upload"), entity, String.class);

        // Then: Response should contain correct counts
        assertThat(response.getBody()).contains("New artifacts: 2"); // 1 project + 1 dep
        assertThat(response.getBody()).contains("New edges: 1"); // 1 dependency edge
    }
}
