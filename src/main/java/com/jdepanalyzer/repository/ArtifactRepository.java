package com.jdepanalyzer.repository;

import com.jdepanalyzer.model.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, String> {

    List<Artifact> findByArtifactIdContainingIgnoreCase(String artifactId);

    List<Artifact> findByGroupIdContainingIgnoreCase(String groupId);
}
