package com.jdepanalyzer.repository;

import com.jdepanalyzer.model.DependencyEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DependencyEdgeRepository extends JpaRepository<DependencyEdge, Long> {

    List<DependencyEdge> findByFromGav(String fromGav);

    List<DependencyEdge> findByToGav(String toGav);

    List<DependencyEdge> findByScopeIn(List<String> scopes);

    Optional<DependencyEdge> findByFromGavAndToGavAndScopeAndOptional(
            String fromGav, String toGav, String scope, Boolean optional);

    @Query("SELECT DISTINCT d.scope FROM DependencyEdge d WHERE d.scope IS NOT NULL")
    List<String> findDistinctScopes();

    boolean existsByFromGavAndToGavAndScopeAndOptional(
            String fromGav, String toGav, String scope, Boolean optional);
}
