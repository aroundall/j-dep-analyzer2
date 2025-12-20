package com.jdepanalyzer.service;

import com.jdepanalyzer.dto.GAV;
import com.jdepanalyzer.model.Artifact;
import com.jdepanalyzer.model.DependencyEdge;
import com.jdepanalyzer.repository.ArtifactRepository;
import com.jdepanalyzer.repository.DependencyEdgeRepository;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for graph operations using JGraphT.
 * Mirrors Python's graph.py logic.
 */
@Service
public class GraphService {

    private final ArtifactRepository artifactRepository;
    private final DependencyEdgeRepository edgeRepository;

    public GraphService(ArtifactRepository artifactRepository, DependencyEdgeRepository edgeRepository) {
        this.artifactRepository = artifactRepository;
        this.edgeRepository = edgeRepository;
    }

    /**
     * Load the atomic dependency graph from the database.
     * Atomic means: every node is a full group:artifact:version.
     */
    public Graph<String, EdgeData> loadAtomicGraph(Set<String> scopes) {
        Graph<String, EdgeData> graph = new DefaultDirectedGraph<>(EdgeData.class);

        // Add all artifacts as nodes
        for (Artifact artifact : artifactRepository.findAll()) {
            graph.addVertex(artifact.getGav());
        }

        // Add all edges
        List<DependencyEdge> edges;
        if (scopes != null && !scopes.isEmpty()) {
            edges = edgeRepository.findByScopeIn(new ArrayList<>(scopes));
        } else {
            edges = edgeRepository.findAll();
        }

        for (DependencyEdge edge : edges) {
            graph.addVertex(edge.getFromGav());
            graph.addVertex(edge.getToGav());
            graph.addEdge(edge.getFromGav(), edge.getToGav(),
                    new EdgeData(edge.getScope(), edge.getOptional()));
        }

        return graph;
    }

    /**
     * Aggregate nodes by toggling group/version visibility.
     */
    public Graph<String, AggregatedEdgeData> aggregateGraph(
            Graph<String, EdgeData> atomic, boolean showGroup, boolean showVersion) {

        Graph<String, AggregatedEdgeData> out = new DefaultDirectedGraph<>(AggregatedEdgeData.class);
        Map<String, Integer> mergedCount = new HashMap<>();

        // Add nodes with aggregated IDs
        for (String node : atomic.vertexSet()) {
            String newId = aggregatedNodeId(node, showGroup, showVersion);
            if (!out.containsVertex(newId)) {
                out.addVertex(newId);
                mergedCount.put(newId, 0);
            }
            mergedCount.merge(newId, 1, Integer::sum);
        }

        // Add edges with aggregated endpoints
        Map<String, Set<String>> edgeScopes = new HashMap<>();
        Map<String, Boolean> edgeOptional = new HashMap<>();

        for (EdgeData edge : atomic.edgeSet()) {
            String source = atomic.getEdgeSource(edge);
            String target = atomic.getEdgeTarget(edge);
            String uu = aggregatedNodeId(source, showGroup, showVersion);
            String vv = aggregatedNodeId(target, showGroup, showVersion);

            if (uu.equals(vv))
                continue; // Skip self-loops

            String edgeKey = uu + "->" + vv;
            edgeScopes.computeIfAbsent(edgeKey, k -> new HashSet<>())
                    .add(edge.scope != null ? edge.scope : "compile");
            edgeOptional.merge(edgeKey, edge.optional != null && edge.optional, (a, b) -> a || b);

            if (!out.containsEdge(uu, vv)) {
                out.addEdge(uu, vv, new AggregatedEdgeData());
            }
        }

        // Set edge data
        for (AggregatedEdgeData edge : out.edgeSet()) {
            String source = out.getEdgeSource(edge);
            String target = out.getEdgeTarget(edge);
            String edgeKey = source + "->" + target;

            Set<String> scopes = edgeScopes.getOrDefault(edgeKey, Set.of("compile"));
            edge.scope = String.join(", ", new TreeSet<>(scopes));
            edge.optionalAny = edgeOptional.getOrDefault(edgeKey, false);
        }

        return out;
    }

    /**
     * Get nodes within a BFS depth from root.
     */
    public Set<String> nodesWithinDepth(
            Graph<String, ?> graph, String root, String direction, Integer depth) {

        if (root == null || !graph.containsVertex(root)) {
            return new HashSet<>(graph.vertexSet());
        }

        Set<String> result = new HashSet<>();
        result.add(root);

        if (depth == null) {
            // All reachable nodes
            if ("reverse".equals(direction)) {
                collectAncestors(graph, root, result);
            } else {
                collectDescendants(graph, root, result);
            }
        } else {
            // BFS with depth limit
            Queue<Map.Entry<String, Integer>> queue = new LinkedList<>();
            queue.add(Map.entry(root, 0));
            Set<String> visited = new HashSet<>();
            visited.add(root);

            while (!queue.isEmpty()) {
                Map.Entry<String, Integer> entry = queue.poll();
                String node = entry.getKey();
                int dist = entry.getValue();

                if (dist >= depth)
                    continue;

                Set<? extends Object> neighbors = "reverse".equals(direction)
                        ? getIncomingNeighbors(graph, node)
                        : getOutgoingNeighbors(graph, node);

                for (Object nb : neighbors) {
                    String nbStr = nb.toString();
                    if (!visited.contains(nbStr)) {
                        visited.add(nbStr);
                        result.add(nbStr);
                        queue.add(Map.entry(nbStr, dist + 1));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Convert graph to Cytoscape.js elements format.
     * 
     * @param visibleNodes if not null, only include these nodes and edges between
     *                     them
     */
    public List<Map<String, Object>> toCytoscapeElements(
            Graph<String, ?> graph, String rootId, String direction, boolean showVersion,
            Set<String> visibleNodes) {

        List<Map<String, Object>> elements = new ArrayList<>();

        Set<String> highlight = new HashSet<>();
        if (rootId != null && graph.containsVertex(rootId) && "reverse".equals(direction)) {
            collectAncestors(graph, rootId, highlight);
        }

        // Nodes - filter by visibleNodes if provided
        for (String nodeId : graph.vertexSet()) {
            // Skip nodes not in visible set
            if (visibleNodes != null && !visibleNodes.contains(nodeId)) {
                continue;
            }

            // For aggregated graphs, nodeId might not be in GAV format
            // Try to parse, but fall back to using nodeId as label if parsing fails
            GAV gav = GAV.parse(nodeId);

            // If the nodeId doesn't contain colons, it's an aggregated ID (just artifactId)
            // In that case, use the nodeId directly as the label
            boolean isAggregated = !nodeId.contains(":");
            String label;
            if (isAggregated) {
                // For aggregated nodes, the nodeId itself is the label
                label = nodeId;
            } else {
                // nodeId contains colon - could be groupId:artifactId or
                // groupId:artifactId:version
                // Always use the nodeId as the label since it reflects the current aggregation
                // level
                label = nodeId;
            }

            List<String> classes = new ArrayList<>();
            if (rootId != null && nodeId.equals(rootId)) {
                classes.add("root");
            }
            if (highlight.contains(nodeId)) {
                classes.add("highlight");
            }
            if (!showVersion || isAggregated) {
                classes.add("aggregated");
            }

            Map<String, Object> nodeData = new LinkedHashMap<>();
            nodeData.put("id", nodeId);
            nodeData.put("label", label);
            // For aggregated nodes, these fields may not be meaningful
            nodeData.put("group_id", isAggregated ? nodeId : gav.getGroupId());
            nodeData.put("artifact_id", isAggregated ? nodeId : gav.getArtifactId());
            nodeData.put("version", isAggregated ? "(aggregated)" : gav.getVersion());

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("data", nodeData);
            node.put("classes", String.join(" ", classes));

            elements.add(node);
        }

        // Edges - filter by visibleNodes if provided
        for (Object edge : graph.edgeSet()) {
            // Need to use raw type graph access due to wildcard generic
            @SuppressWarnings("unchecked")
            Graph<String, Object> rawGraph = (Graph<String, Object>) graph;
            String source = rawGraph.getEdgeSource(edge);
            String target = rawGraph.getEdgeTarget(edge);

            // Skip edges where source or target is not in visible set
            if (visibleNodes != null &&
                    (!visibleNodes.contains(source) || !visibleNodes.contains(target))) {
                continue;
            }

            String scope = "compile";
            boolean optional = false;

            if (edge instanceof EdgeData ed) {
                scope = ed.scope != null ? ed.scope : "compile";
                optional = ed.optional != null && ed.optional;
            } else if (edge instanceof AggregatedEdgeData aed) {
                scope = aed.scope != null ? aed.scope : "compile";
                optional = aed.optionalAny;
            }

            Map<String, Object> edgeData = new LinkedHashMap<>();
            edgeData.put("id", source + "__" + target);
            edgeData.put("source", source);
            edgeData.put("target", target);
            edgeData.put("scope", scope);
            edgeData.put("optional", optional);

            Map<String, Object> edgeEl = new LinkedHashMap<>();
            edgeEl.put("data", edgeData);

            elements.add(edgeEl);
        }

        return elements;
    }

    /**
     * Build aggregated node ID based on visibility toggles.
     */
    public String aggregatedNodeId(String gav, boolean showGroup, boolean showVersion) {
        GAV parsed = GAV.parse(gav);
        if (showGroup && showVersion) {
            return gav;
        } else if (showGroup) {
            return parsed.getGroupId() + ":" + parsed.getArtifactId();
        } else if (showVersion) {
            return parsed.getArtifactId() + ":" + parsed.getVersion();
        } else {
            return parsed.getArtifactId();
        }
    }

    // Helper methods
    private <E> void collectDescendants(Graph<String, E> graph, String node, Set<String> result) {
        for (E edge : graph.outgoingEdgesOf(node)) {
            String target = graph.getEdgeTarget(edge);
            if (!result.contains(target)) {
                result.add(target);
                collectDescendants(graph, target, result);
            }
        }
    }

    private <E> void collectAncestors(Graph<String, E> graph, String node, Set<String> result) {
        for (E edge : graph.incomingEdgesOf(node)) {
            String source = graph.getEdgeSource(edge);
            if (!result.contains(source)) {
                result.add(source);
                collectAncestors(graph, source, result);
            }
        }
    }

    private <E> Set<String> getOutgoingNeighbors(Graph<String, E> graph, String node) {
        Set<String> neighbors = new HashSet<>();
        for (E edge : graph.outgoingEdgesOf(node)) {
            neighbors.add(graph.getEdgeTarget(edge));
        }
        return neighbors;
    }

    private <E> Set<String> getIncomingNeighbors(Graph<String, E> graph, String node) {
        Set<String> neighbors = new HashSet<>();
        for (E edge : graph.incomingEdgesOf(node)) {
            neighbors.add(graph.getEdgeSource(edge));
        }
        return neighbors;
    }

    // Edge data classes
    public static class EdgeData extends DefaultEdge {
        public String scope;
        public Boolean optional;

        public EdgeData() {
        }

        public EdgeData(String scope, Boolean optional) {
            this.scope = scope;
            this.optional = optional;
        }
    }

    public static class AggregatedEdgeData extends DefaultEdge {
        public String scope;
        public boolean optionalAny;
    }
}
