package com.jdepanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Maven coordinates (GroupId, ArtifactId, Version).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GAV {

    public static final String UNKNOWN_VERSION = "Unknown";

    private String groupId;
    private String artifactId;
    private String version;

    /**
     * Return a compact string representation (groupId:artifactId:version).
     */
    public String compact() {
        return groupId + ":" + artifactId + ":" + version;
    }

    /**
     * Parse a GAV string.
     */
    public static GAV parse(String gav) {
        String[] parts = (gav != null ? gav : "").split(":", 3);
        String groupId = parts.length > 0 && !parts[0].isEmpty() ? parts[0] : "Unknown";
        String artifactId = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : "Unknown";
        String version = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : UNKNOWN_VERSION;
        return new GAV(groupId, artifactId, version);
    }
}
