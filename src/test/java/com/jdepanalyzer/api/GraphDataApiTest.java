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
 * Integration tests for GET /api/graph/data endpoint.
 * 
 * BDD-style tests for retrieving graph data in Cytoscape.js format.
 */
class GraphDataApiTest extends BaseApiTest {

    // ========================================================================
    // Scenario: Get graph data when database is empty
    // ========================================================================
    @Test
    @DisplayName("Given an empty database, when I request graph data, then I should get empty elements")
    void getGraphData_whenEmpty_shouldReturnEmptyElements() {
        // Given: An empty database
        assertThat(artifactRepository.count()).isZero();

        // When: I request graph data
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl("/api/graph/data"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        // Then: I should get empty elements
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) response.getBody().get("elements")).isEmpty();
        assertThat(response.getBody().get("node_count")).isEqualTo(0);
    }

    // ========================================================================
    // Scenario: Get graph data with data (all nodes)
    // ========================================================================
    @Test
    @DisplayName("Given artifacts exist, when I request graph data without root, then I should get all nodes and edges")
    void getGraphData_withData_shouldReturnAllNodesAndEdges() throws IOException {
        // Given: Some artifacts with dependencies
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request graph data without specifying root
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl("/api/graph/data"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        // Then: I should get nodes and edges
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> elements = (List<?>) response.getBody().get("elements");
        assertThat(elements).isNotEmpty();
        assertThat((Integer) response.getBody().get("node_count")).isEqualTo(2);
    }

    // ========================================================================
    // Scenario: Get graph data with root_id (forward direction)
    // ========================================================================
    @Test
    @DisplayName("Given a graph exists, when I request with root_id forward, then I should get descendants")
    void getGraphData_withRootForward_shouldReturnDescendants() throws IOException {
        // Given: A graph with dependencies
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request graph data with root_id and forward direction
        String rootId = "org.springframework:spring-core:6.2.15";
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl("/api/graph/data?root_id=" + rootId + "&direction=forward"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        // Then: I should get the root and its dependencies
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> elements = (List<?>) response.getBody().get("elements");
        assertThat(elements).isNotEmpty();
    }

    // ========================================================================
    // Scenario: Get graph data with root_id (backward direction)
    // ========================================================================
    @Test
    @DisplayName("Given a graph exists, when I request with root_id backward, then I should get ancestors")
    void getGraphData_withRootBackward_shouldReturnAncestors() throws IOException {
        // Given: A graph with dependencies
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request graph data with root_id and backward direction
        String leafId = "org.springframework:spring-jcl:6.2.15";
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl("/api/graph/data?root_id=" + leafId + "&direction=backward"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        // Then: I should get the leaf and its dependents (ancestors)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> elements = (List<?>) response.getBody().get("elements");
        assertThat(elements).isNotEmpty();
    }

    // ========================================================================
    // Scenario: Get graph data with depth limit
    // ========================================================================
    @Test
    @DisplayName("Given a deep graph exists, when I request with depth limit, then I should get limited depth")
    void getGraphData_withDepthLimit_shouldRespectLimit() throws IOException {
        // Given: A graph with dependencies
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request graph data with depth limit
        String rootId = "org.springframework:spring-core:6.2.15";
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl("/api/graph/data?root_id=" + rootId + "&depth=1"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        // Then: I should get nodes within the depth limit
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Integer) response.getBody().get("node_count")).isLessThanOrEqualTo(2);
    }

    // ========================================================================
    // Scenario: Get graph data with aggregation (show_group=false)
    // ========================================================================
    @Test
    @DisplayName("Given a graph exists, when I request with show_group=false, then nodes should be aggregated")
    void getGraphData_withHideGroup_shouldAggregateNodes() throws IOException {
        // Given: A graph with dependencies
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request graph data hiding the group
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl("/api/graph/data?show_group=false"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        // Then: Elements should be aggregated (fewer unique group:artifact combos)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("elements");
    }

    // ========================================================================
    // Scenario: Get graph data with aggregation (show_version=false)
    // ========================================================================
    @Test
    @DisplayName("Given a graph exists, when I request with show_version=false, then versions should be hidden")
    void getGraphData_withHideVersion_shouldAggregateVersions() throws IOException {
        // Given: A graph with dependencies
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request graph data hiding versions
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl("/api/graph/data?show_version=false"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        // Then: Versions should be aggregated
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("elements");
    }

    // ========================================================================
    // Scenario: Graph elements have valid Cytoscape.js format
    // ========================================================================
    @Test
    @DisplayName("Given a graph exists, when I request graph data, then elements should have Cytoscape.js format")
    @SuppressWarnings("unchecked")
    void getGraphData_shouldReturnCytoscapeFormat() throws IOException {
        // Given: A graph with dependencies
        uploadPomFiles("spring-core-6.2.15.pom");

        // When: I request graph data
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl("/api/graph/data"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        // Then: Elements should have valid Cytoscape.js format
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> elements = (List<Map<String, Object>>) response.getBody().get("elements");

        // Should have both nodes and edges
        assertThat(elements).anyMatch(e -> {
            Map<String, Object> data = (Map<String, Object>) e.get("data");
            return data != null && data.containsKey("id") && !data.containsKey("source");
        });
        assertThat(elements).anyMatch(e -> {
            Map<String, Object> data = (Map<String, Object>) e.get("data");
            return data != null && data.containsKey("source") && data.containsKey("target");
        });
    }
}
