package com.jdepanalyzer.controller;

import com.jdepanalyzer.model.Artifact;
import com.jdepanalyzer.repository.ArtifactRepository;
import com.jdepanalyzer.repository.DependencyEdgeRepository;
import com.jdepanalyzer.service.GraphService;
import com.jdepanalyzer.service.UploadService;
import org.jgrapht.Graph;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * REST API controller for data operations.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final ArtifactRepository artifactRepository;
    private final DependencyEdgeRepository edgeRepository;
    private final GraphService graphService;
    private final UploadService uploadService;

    public ApiController(ArtifactRepository artifactRepository,
            DependencyEdgeRepository edgeRepository,
            GraphService graphService,
            UploadService uploadService) {
        this.artifactRepository = artifactRepository;
        this.edgeRepository = edgeRepository;
        this.graphService = graphService;
        this.uploadService = uploadService;
    }

    /**
     * Upload POM files.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadPoms(@RequestParam("files") List<MultipartFile> files) {
        Map<String, Object> response = new LinkedHashMap<>();

        if (files == null || files.isEmpty()) {
            response.put("success", false);
            response.put("error", "No files provided");
            return ResponseEntity.badRequest().body(response);
        }

        UploadService.UploadResult result = uploadService.processUpload(files);

        response.put("success", true);
        response.put("parsed", result.parsed());
        response.put("newArtifacts", result.newArtifacts());
        response.put("newEdges", result.newEdges());
        response.put("skipped", result.skipped());
        response.put("errors", result.errors());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all artifacts.
     */
    @GetMapping("/artifacts")
    public ResponseEntity<List<Map<String, Object>>> getArtifacts(
            @RequestParam(defaultValue = "500") int limit) {
        List<Artifact> artifacts = artifactRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        int count = 0;
        for (Artifact a : artifacts) {
            if (count >= limit)
                break;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("gav", a.getGav());
            item.put("group_id", a.getGroupId());
            item.put("artifact_id", a.getArtifactId());
            item.put("version", a.getVersion());
            result.add(item);
            count++;
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get graph data in Cytoscape.js format.
     */
    @GetMapping("/graph/data")
    public ResponseEntity<Map<String, Object>> getGraphData(
            @RequestParam(name = "root_id", required = false) String rootId,
            @RequestParam(defaultValue = "forward") String direction,
            @RequestParam(name = "show_group", defaultValue = "true") boolean showGroup,
            @RequestParam(name = "show_version", defaultValue = "true") boolean showVersion,
            @RequestParam(required = false) Integer depth,
            @RequestParam(required = false) List<String> scope) {

        Set<String> scopes = scope != null ? new HashSet<>(scope) : null;

        // Load atomic graph
        Graph<String, GraphService.EdgeData> atomic = graphService.loadAtomicGraph(scopes);

        // Aggregate if needed
        Graph<String, ?> graph;
        if (!showGroup || !showVersion) {
            graph = graphService.aggregateGraph(atomic, showGroup, showVersion);
            // Adjust rootId for aggregation
            if (rootId != null) {
                rootId = graphService.aggregatedNodeId(rootId, showGroup, showVersion);
            }
        } else {
            graph = atomic;
        }

        // Filter by depth and direction
        Set<String> visibleNodes = null;
        if (rootId != null && graph.containsVertex(rootId)) {
            // Get nodes within depth (or all reachable nodes if depth is null)
            visibleNodes = graphService.nodesWithinDepth(graph, rootId, direction, depth);
        }

        List<Map<String, Object>> elements = graphService.toCytoscapeElements(
                graph, rootId, direction, showVersion, visibleNodes);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("elements", elements);
        response.put("node_count", visibleNodes != null ? visibleNodes.size() : graph.vertexSet().size());
        response.put("edge_count", graph.edgeSet().size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get dependencies table data as JSON.
     */
    @GetMapping("/dependencies/table")
    public ResponseEntity<List<Map<String, Object>>> getDependenciesTable(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "group_q", required = false) String groupQuery,
            @RequestParam(name = "scope", required = false) List<String> scopes,
            @RequestParam(name = "ignore_version", defaultValue = "false") boolean ignoreVersion,
            @RequestParam(name = "ignore_group", defaultValue = "false") boolean ignoreGroup,
            @RequestParam(defaultValue = "500") int limit) {

        var edges = edgeRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        int count = 0;
        for (var edge : edges) {
            if (count >= limit)
                break;

            // Parse from and to GAVs
            String[] fromParts = edge.getFromGav().split(":", 3);
            String[] toParts = edge.getToGav().split(":", 3);

            String fromGroup = fromParts.length > 0 ? fromParts[0] : "";
            String fromArtifact = fromParts.length > 1 ? fromParts[1] : "";
            String fromVersion = fromParts.length > 2 ? fromParts[2] : "";

            String toGroup = toParts.length > 0 ? toParts[0] : "";
            String toArtifact = toParts.length > 1 ? toParts[1] : "";
            String toVersion = toParts.length > 2 ? toParts[2] : "";

            // Apply filters
            if (query != null && !query.isEmpty()) {
                String lowerQuery = query.toLowerCase();
                if (!fromArtifact.toLowerCase().contains(lowerQuery) &&
                        !toArtifact.toLowerCase().contains(lowerQuery)) {
                    continue;
                }
            }
            if (groupQuery != null && !groupQuery.isEmpty()) {
                String lowerGroupQuery = groupQuery.toLowerCase();
                if (!fromGroup.toLowerCase().contains(lowerGroupQuery) &&
                        !toGroup.toLowerCase().contains(lowerGroupQuery)) {
                    continue;
                }
            }
            if (scopes != null && !scopes.isEmpty() && edge.getScope() != null) {
                if (!scopes.contains(edge.getScope())) {
                    continue;
                }
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("fromGav", edge.getFromGav());
            row.put("fromGroup", ignoreGroup ? "" : fromGroup);
            row.put("fromArtifact", fromArtifact);
            row.put("fromVersion", ignoreVersion ? "" : fromVersion);
            row.put("toGroup", ignoreGroup ? "" : toGroup);
            row.put("toArtifact", toArtifact);
            row.put("toVersion", ignoreVersion ? "" : toVersion);
            row.put("scope", edge.getScope() != null ? edge.getScope() : "compile");
            result.add(row);
            count++;
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Export dependencies as CSV (matches Python version format).
     */
    @GetMapping("/dependencies/export")
    public void exportDependenciesCsv(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "group_q", required = false) String groupQuery,
            @RequestParam(name = "scope", required = false) List<String> scopes,
            @RequestParam(name = "ignore_version", defaultValue = "false") boolean ignoreVersion,
            @RequestParam(name = "ignore_group", defaultValue = "false") boolean ignoreGroup,
            @RequestParam(name = "limit", required = false) Integer limit,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"dependencies.csv\"");

        var writer = response.getWriter();

        // CSV header matching Python version
        writer.println("source_group,source_artifact,source_version,target_group,target_artifact,target_version,scope");

        var edges = edgeRepository.findAll();
        int count = 0;
        int maxCount = limit != null ? limit : Integer.MAX_VALUE;

        for (var edge : edges) {
            if (count >= maxCount)
                break;

            // Parse from and to GAVs
            String[] fromParts = edge.getFromGav().split(":", 3);
            String[] toParts = edge.getToGav().split(":", 3);

            String fromGroup = fromParts.length > 0 ? fromParts[0] : "";
            String fromArtifact = fromParts.length > 1 ? fromParts[1] : "";
            String fromVersion = fromParts.length > 2 ? fromParts[2] : "";

            String toGroup = toParts.length > 0 ? toParts[0] : "";
            String toArtifact = toParts.length > 1 ? toParts[1] : "";
            String toVersion = toParts.length > 2 ? toParts[2] : "";

            String scopeVal = edge.getScope() != null ? edge.getScope() : "compile";

            // Apply filters
            if (query != null && !query.isEmpty()) {
                String lowerQuery = query.toLowerCase();
                if (!fromArtifact.toLowerCase().contains(lowerQuery) &&
                        !toArtifact.toLowerCase().contains(lowerQuery)) {
                    continue;
                }
            }
            if (groupQuery != null && !groupQuery.isEmpty()) {
                String lowerGroupQuery = groupQuery.toLowerCase();
                if (!fromGroup.toLowerCase().contains(lowerGroupQuery) &&
                        !toGroup.toLowerCase().contains(lowerGroupQuery)) {
                    continue;
                }
            }
            if (scopes != null && !scopes.isEmpty()) {
                if (!scopes.contains(scopeVal)) {
                    continue;
                }
            }

            // Apply ignore flags for display
            String displayFromGroup = ignoreGroup ? "" : fromGroup;
            String displayFromVersion = ignoreVersion ? "" : fromVersion;
            String displayToGroup = ignoreGroup ? "" : toGroup;
            String displayToVersion = ignoreVersion ? "" : toVersion;

            writer.println(String.join(",",
                    escapeCsv(displayFromGroup),
                    escapeCsv(fromArtifact),
                    escapeCsv(displayFromVersion),
                    escapeCsv(displayToGroup),
                    escapeCsv(toArtifact),
                    escapeCsv(displayToVersion),
                    escapeCsv(scopeVal)));
            count++;
        }

        writer.flush();
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
