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
    public ResponseEntity<String> uploadPoms(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body("<div class=\"text-red-500\">No files provided</div>");
        }

        UploadService.UploadResult result = uploadService.processUpload(files);

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"p-4 rounded-lg bg-green-50 border border-green-200\">");
        html.append("<p class=\"text-green-700 font-medium\">Upload Complete</p>");
        html.append("<ul class=\"text-sm text-green-600 mt-2\">");
        html.append("<li>Parsed: ").append(result.parsed()).append(" files</li>");
        html.append("<li>New artifacts: ").append(result.newArtifacts()).append("</li>");
        html.append("<li>New edges: ").append(result.newEdges()).append("</li>");
        if (result.skipped() > 0) {
            html.append("<li class=\"text-yellow-600\">Skipped: ").append(result.skipped()).append("</li>");
        }
        html.append("</ul>");

        if (!result.errors().isEmpty()) {
            html.append("<div class=\"mt-2 text-red-500 text-sm\">");
            html.append("<p class=\"font-medium\">Errors:</p>");
            for (String error : result.errors()) {
                html.append("<p>").append(escapeHtml(error)).append("</p>");
            }
            html.append("</div>");
        }
        html.append("</div>");

        return ResponseEntity.ok(html.toString());
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

        // Filter by depth
        if (rootId != null && depth != null) {
            Set<String> visibleNodes = graphService.nodesWithinDepth(graph, rootId, direction, depth);
            // Create subgraph (simplified - just filter elements in output)
        }

        List<Map<String, Object>> elements = graphService.toCytoscapeElements(graph, rootId, direction, showVersion);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("elements", elements);
        response.put("node_count", graph.vertexSet().size());
        response.put("edge_count", graph.edgeSet().size());

        return ResponseEntity.ok(response);
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
