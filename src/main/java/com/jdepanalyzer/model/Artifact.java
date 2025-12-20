package com.jdepanalyzer.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Maven artifact stored as a single row.
 * Uses gav (group:artifact:version) as the primary key.
 */
@Entity
@Table(name = "artifact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artifact {

    @Id
    private String gav; // "group:artifact:version"

    private String groupId;

    private String artifactId;

    private String version;

    /**
     * Create an Artifact from GAV components.
     */
    public static Artifact of(String groupId, String artifactId, String version) {
        String gav = groupId + ":" + artifactId + ":" + version;
        return Artifact.builder()
                .gav(gav)
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .build();
    }

    /**
     * Parse a GAV string into an Artifact.
     */
    public static Artifact fromGav(String gav) {
        String[] parts = gav.split(":", 3);
        String groupId = parts.length > 0 ? parts[0] : "Unknown";
        String artifactId = parts.length > 1 ? parts[1] : "Unknown";
        String version = parts.length > 2 ? parts[2] : "Unknown";
        return Artifact.of(groupId, artifactId, version);
    }
}
