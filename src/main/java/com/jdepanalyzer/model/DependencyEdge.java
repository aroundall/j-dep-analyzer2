package com.jdepanalyzer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A dependency edge between two artifacts (A -> B means A depends on B).
 */
@Entity
@Table(name = "dependencyedge", uniqueConstraints = {
        @UniqueConstraint(name = "uq_dep_edge", columnNames = { "from_gav", "to_gav", "scope", "optional" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DependencyEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_gav")
    private String fromGav;

    @Column(name = "to_gav")
    private String toGav;

    private String scope;

    private Boolean optional;

    /**
     * Create a new dependency edge.
     */
    public static DependencyEdge of(String fromGav, String toGav, String scope, Boolean optional) {
        return DependencyEdge.builder()
                .fromGav(fromGav)
                .toGav(toGav)
                .scope(scope != null ? scope : "compile")
                .optional(optional)
                .build();
    }
}
