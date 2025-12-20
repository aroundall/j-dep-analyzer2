package com.jdepanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parsed Maven project.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MavenProject {

    private GAV project;

    @Builder.Default
    private List<Dependency> dependencies = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Dependency {
        private GAV gav;
        private String scope;
        private Boolean optional;
    }
}
