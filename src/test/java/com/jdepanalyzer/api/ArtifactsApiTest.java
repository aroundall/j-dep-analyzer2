package com.jdepanalyzer.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for GET /api/artifacts endpoint.
 * 
 * BDD-style tests for retrieving artifact data.
 */
class ArtifactsApiTest extends BaseApiTest {

    // ========================================================================
    // Scenario: Get artifacts when database is empty
    // ========================================================================
    @Test
    @DisplayName("Given an empty database, when I request artifacts, then I should get an empty list")
    void getArtifacts_whenDatabaseEmpty_shouldReturnEmptyList() {
        // Given: An empty database
        assertThat(artifactRepository.count()).isZero();

        // When: I request all artifacts
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                apiUrl("/api/artifacts"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        // Then: I should get an empty list
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ========================================================================
    // Scenario: Get artifacts with data
    // ========================================================================
    @Test
    @DisplayName("Given artifacts exist, when I request artifacts, then I should get all of them")
    void getArtifacts_withData_shouldReturnAllArtifacts() throws IOException {
        // Given: Some artifacts in the database (spring-core has 1 dependency)
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request all artifacts
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                apiUrl("/api/artifacts"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        // Then: I should get all artifacts (spring-core + spring-jcl)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);

        // And: Each artifact should have the expected fields
        Map<String, Object> artifact = response.getBody().get(0);
        assertThat(artifact).containsKeys("gav", "group_id", "artifact_id", "version");
    }

    // ========================================================================
    // Scenario: Get artifacts with limit parameter
    // ========================================================================
    @Test
    @DisplayName("Given many artifacts exist, when I request with limit, then I should get at most that many")
    void getArtifacts_withLimit_shouldRespectLimit() throws IOException {
        // Given: Multiple artifacts in the database
        uploadPomFiles("spring-core-6.2.15.pom", "spring-context-6.2.15.pom");

        // When: I request artifacts with a limit of 2
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                apiUrl("/api/artifacts?limit=2"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        // Then: I should get at most 2 artifacts
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    // ========================================================================
    // Scenario: Artifact data contains correct GAV components
    // ========================================================================
    @Test
    @DisplayName("Given an artifact exists, when I request artifacts, then GAV components should be correct")
    void getArtifacts_shouldHaveCorrectGavComponents() throws IOException {
        // Given: A known artifact
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request artifacts
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                apiUrl("/api/artifacts"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        // Then: The spring-core artifact should have correct components
        assertThat(response.getBody()).anySatisfy(artifact -> {
            assertThat(artifact.get("gav")).isEqualTo("org.springframework:spring-core:6.2.15");
            assertThat(artifact.get("group_id")).isEqualTo("org.springframework");
            assertThat(artifact.get("artifact_id")).isEqualTo("spring-core");
            assertThat(artifact.get("version")).isEqualTo("6.2.15");
        });
    }
}
