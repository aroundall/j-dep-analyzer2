package com.jdepanalyzer.api;

import com.jdepanalyzer.repository.ArtifactRepository;
import com.jdepanalyzer.repository.DependencyEdgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;

/**
 * Base class for API integration tests.
 * 
 * Each test class extends this to get:
 * - Spring context with real endpoints
 * - SQLite in-memory database (isolated per test)
 * - Common setup/cleanup methods
 * - Utility methods for loading test POMs
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseApiTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ArtifactRepository artifactRepository;

    @Autowired
    protected DependencyEdgeRepository edgeRepository;

    /**
     * Clean up database before each test to ensure independence.
     */
    @BeforeEach
    void cleanDatabase() {
        edgeRepository.deleteAll();
        artifactRepository.deleteAll();
    }

    /**
     * Get the base URL for API calls.
     */
    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Get the full URL for an API endpoint.
     */
    protected String apiUrl(String path) {
        return baseUrl() + path;
    }

    /**
     * Load a sample POM file as a Resource.
     */
    protected Resource loadSamplePom(String filename) {
        return new ClassPathResource("testing/sample-poms/" + filename);
    }

    /**
     * Create HTTP entity for multipart file upload.
     */
    protected HttpEntity<MultiValueMap<String, Object>> createUploadEntity(String... pomFilenames)
            throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (String filename : pomFilenames) {
            Resource resource = loadSamplePom(filename);
            body.add("files", resource);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(body, headers);
    }

    /**
     * Upload POM files via the API.
     * Helper method for tests that need data setup.
     */
    protected void uploadPomFiles(String... pomFilenames) throws IOException {
        HttpEntity<MultiValueMap<String, Object>> entity = createUploadEntity(pomFilenames);
        restTemplate.postForEntity(apiUrl("/api/upload"), entity, String.class);
    }
}
