package com.jdepanalyzer.controller;

import com.jdepanalyzer.model.Artifact;
import com.jdepanalyzer.model.DependencyEdge;
import com.jdepanalyzer.repository.ArtifactRepository;
import com.jdepanalyzer.repository.DependencyEdgeRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Controller for CSV exports.
 */
@Controller
public class ExportController {

    private final ArtifactRepository artifactRepository;
    private final DependencyEdgeRepository edgeRepository;

    public ExportController(ArtifactRepository artifactRepository,
            DependencyEdgeRepository edgeRepository) {
        this.artifactRepository = artifactRepository;
        this.edgeRepository = edgeRepository;
    }

    @GetMapping("/export/{table}.csv")
    public void exportTableCsv(@PathVariable String table, HttpServletResponse response) throws IOException {
        // Validate table name
        if (!table.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            response.sendError(400, "Invalid table name");
            return;
        }

        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + table + ".csv\"");

        PrintWriter writer = response.getWriter();

        switch (table.toLowerCase()) {
            case "artifact" -> exportArtifacts(writer);
            case "dependencyedge" -> exportEdges(writer);
            default -> {
                response.sendError(404, "Table not found: " + table);
                return;
            }
        }

        writer.flush();
    }

    private void exportArtifacts(PrintWriter writer) {
        writer.println("gav,group_id,artifact_id,version");

        List<Artifact> artifacts = artifactRepository.findAll();
        for (Artifact a : artifacts) {
            writer.println(String.join(",",
                    escapeCsv(a.getGav()),
                    escapeCsv(a.getGroupId()),
                    escapeCsv(a.getArtifactId()),
                    escapeCsv(a.getVersion())));
        }
    }

    private void exportEdges(PrintWriter writer) {
        writer.println("id,from_gav,to_gav,scope,optional");

        List<DependencyEdge> edges = edgeRepository.findAll();
        for (DependencyEdge e : edges) {
            writer.println(String.join(",",
                    String.valueOf(e.getId()),
                    escapeCsv(e.getFromGav()),
                    escapeCsv(e.getToGav()),
                    escapeCsv(e.getScope()),
                    String.valueOf(e.getOptional())));
        }
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
