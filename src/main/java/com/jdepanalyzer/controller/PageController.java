package com.jdepanalyzer.controller;

import com.jdepanalyzer.dto.GAV;
import com.jdepanalyzer.model.Artifact;
import com.jdepanalyzer.model.DependencyEdge;
import com.jdepanalyzer.repository.ArtifactRepository;
import com.jdepanalyzer.repository.DependencyEdgeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller for HTML pages.
 */
@Controller
public class PageController {

    private final ArtifactRepository artifactRepository;
    private final DependencyEdgeRepository edgeRepository;

    public PageController(ArtifactRepository artifactRepository,
            DependencyEdgeRepository edgeRepository) {
        this.artifactRepository = artifactRepository;
        this.edgeRepository = edgeRepository;
    }

    /**
     * Dashboard page with upload widget and global graph.
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Dashboard - J-Dep Analyzer");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("showGroup", false);
        model.addAttribute("showVersion", false);
        return "index";
    }

    /**
     * Dependencies list page.
     */
    @GetMapping("/page/dependencies/list")
    public String dependenciesList(Model model) {
        List<String> scopes = edgeRepository.findDistinctScopes();
        model.addAttribute("title", "Dependencies List - J-Dep Analyzer");
        model.addAttribute("activePage", "dependencies");
        model.addAttribute("scopes", scopes);
        return "dependencies_list";
    }

    /**
     * Visualization page centered on a specific artifact.
     */
    @GetMapping("/page/visualize/{rootId}")
    public String visualize(@PathVariable String rootId,
            @RequestParam(required = false) List<String> scope,
            Model model) {
        String decodedRootId = URLDecoder.decode(rootId, StandardCharsets.UTF_8);
        GAV gav = GAV.parse(decodedRootId);

        model.addAttribute("title", gav.getArtifactId() + " - J-Dep Analyzer");
        model.addAttribute("activePage", "visualize");
        model.addAttribute("rootId", decodedRootId);
        model.addAttribute("groupId", gav.getGroupId());
        model.addAttribute("artifactId", gav.getArtifactId());
        model.addAttribute("version", gav.getVersion());
        model.addAttribute("selectedScopes", scope);
        model.addAttribute("wide", true);

        List<String> scopes = edgeRepository.findDistinctScopes();
        model.addAttribute("scopes", scopes);

        return "visualize";
    }

    /**
     * Redirect /details to /visualize (for backwards compatibility).
     */
    @GetMapping("/page/details/{rootId}")
    public String details(@PathVariable String rootId,
            @RequestParam(required = false) List<String> scope) {
        String scopeParam = scope != null && !scope.isEmpty()
                ? "?scope=" + String.join("&scope=", scope)
                : "";
        return "redirect:/page/visualize/" + rootId + scopeParam;
    }

    /**
     * Export page.
     */
    @GetMapping("/page/export")
    public String exportPage(Model model) {
        model.addAttribute("title", "Export - J-Dep Analyzer");
        model.addAttribute("activePage", "export");

        long artifactCount = artifactRepository.count();
        long edgeCount = edgeRepository.count();

        model.addAttribute("tables", List.of(
                new TableInfo("artifact", artifactCount),
                new TableInfo("dependencyedge", edgeCount)));

        return "export";
    }

    public record TableInfo(String name, long rowCount) {
    }
}
