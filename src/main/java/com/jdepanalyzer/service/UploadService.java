package com.jdepanalyzer.service;

import com.jdepanalyzer.dto.MavenProject;
import com.jdepanalyzer.model.Artifact;
import com.jdepanalyzer.model.DependencyEdge;
import com.jdepanalyzer.repository.ArtifactRepository;
import com.jdepanalyzer.repository.DependencyEdgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to handle POM file uploads and data persistence.
 */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final PomParser pomParser;
    private final ArtifactRepository artifactRepository;
    private final DependencyEdgeRepository edgeRepository;

    public UploadService(PomParser pomParser,
            ArtifactRepository artifactRepository,
            DependencyEdgeRepository edgeRepository) {
        this.pomParser = pomParser;
        this.artifactRepository = artifactRepository;
        this.edgeRepository = edgeRepository;
    }

    /**
     * Process uploaded POM files and store artifacts/dependencies.
     */
    @Transactional
    public UploadResult processUpload(List<MultipartFile> files) {
        int parsed = 0;
        int skipped = 0;
        int newArtifacts = 0;
        int newEdges = 0;
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            try {
                MavenProject project = pomParser.parse(file.getInputStream());

                // Upsert project artifact
                Artifact projectArtifact = Artifact.of(
                        project.getProject().getGroupId(),
                        project.getProject().getArtifactId(),
                        project.getProject().getVersion());

                if (!artifactRepository.existsById(projectArtifact.getGav())) {
                    artifactRepository.save(projectArtifact);
                    newArtifacts++;
                }

                // Process dependencies
                for (MavenProject.Dependency dep : project.getDependencies()) {
                    // Upsert dependency artifact
                    Artifact depArtifact = Artifact.of(
                            dep.getGav().getGroupId(),
                            dep.getGav().getArtifactId(),
                            dep.getGav().getVersion());

                    if (!artifactRepository.existsById(depArtifact.getGav())) {
                        artifactRepository.save(depArtifact);
                        newArtifacts++;
                    }

                    // Create edge if not exists
                    String fromGav = projectArtifact.getGav();
                    String toGav = depArtifact.getGav();
                    String scope = dep.getScope() != null ? dep.getScope() : "compile";

                    if (!edgeRepository.existsByFromGavAndToGavAndScopeAndOptional(
                            fromGav, toGav, scope, dep.getOptional())) {
                        DependencyEdge edge = DependencyEdge.of(fromGav, toGav, scope, dep.getOptional());
                        edgeRepository.save(edge);
                        newEdges++;
                    }
                }

                parsed++;
                log.info("Parsed POM: {} -> {} dependencies",
                        project.getProject().compact(), project.getDependencies().size());

            } catch (Exception e) {
                log.warn("Failed to parse {}: {}", filename, e.getMessage());
                errors.add(filename + ": " + e.getMessage());
                skipped++;
            }
        }

        return new UploadResult(parsed, skipped, newArtifacts, newEdges, errors);
    }

    public record UploadResult(
            int parsed,
            int skipped,
            int newArtifacts,
            int newEdges,
            List<String> errors) {
    }
}
