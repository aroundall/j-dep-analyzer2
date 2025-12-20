package com.jdepanalyzer.service;

import com.jdepanalyzer.dto.GAV;
import com.jdepanalyzer.dto.MavenProject;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to parse Maven pom.xml files.
 * Mirrors Python's parser.py logic.
 */
@Service
public class PomParser {

    private static final Pattern PLACEHOLDER_RE = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * Parse a POM XML from an InputStream.
     */
    public MavenProject parse(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();

        return parseDocument(doc);
    }

    /**
     * Parse a POM XML from a String.
     */
    public MavenProject parse(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
        doc.getDocumentElement().normalize();

        return parseDocument(doc);
    }

    private MavenProject parseDocument(Document doc) throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();

        // Extract project coordinates
        String rawGroupId = getTextContent(doc, "/project/groupId", xPath);
        String rawArtifactId = getTextContent(doc, "/project/artifactId", xPath);
        String rawVersion = getTextContent(doc, "/project/version", xPath);

        // Extract parent coordinates
        String parentGroupId = getTextContent(doc, "/project/parent/groupId", xPath);
        String parentArtifactId = getTextContent(doc, "/project/parent/artifactId", xPath);
        String parentVersion = getTextContent(doc, "/project/parent/version", xPath);

        // Inherit from parent if missing
        if (rawGroupId == null || rawGroupId.isEmpty()) {
            rawGroupId = parentGroupId;
        }
        if (rawVersion == null || rawVersion.isEmpty()) {
            rawVersion = parentVersion;
        }

        if (rawArtifactId == null || rawArtifactId.isEmpty()) {
            throw new IllegalArgumentException("Missing required <artifactId> in pom.xml");
        }
        if (rawGroupId == null || rawGroupId.isEmpty()) {
            throw new IllegalArgumentException("Missing required <groupId> (or parent <groupId>) in pom.xml");
        }

        // Parse properties
        Map<String, String> props = parseProperties(doc, xPath);

        // Add built-in properties
        String effectiveVersion = rawVersion != null ? rawVersion : GAV.UNKNOWN_VERSION;
        props.put("project.groupId", rawGroupId);
        props.put("project.artifactId", rawArtifactId);
        props.put("project.version", effectiveVersion);
        props.put("pom.groupId", rawGroupId);
        props.put("pom.artifactId", rawArtifactId);
        props.put("pom.version", effectiveVersion);
        props.put("groupId", rawGroupId);
        props.put("artifactId", rawArtifactId);
        props.put("version", effectiveVersion);

        // Resolve placeholders
        String groupId = resolvePlaceholders(rawGroupId, props);
        String version = normalizeVersion(effectiveVersion, props);

        GAV projectGav = GAV.builder()
                .groupId(groupId)
                .artifactId(rawArtifactId)
                .version(version)
                .build();

        List<MavenProject.Dependency> deps = new ArrayList<>();

        // Treat <parent> as a dependency edge
        if (parentGroupId != null && parentArtifactId != null) {
            String resolvedParentGroup = resolvePlaceholders(parentGroupId, props);
            String resolvedParentVersion = normalizeVersion(parentVersion, props);
            GAV parentGav = GAV.builder()
                    .groupId(resolvedParentGroup)
                    .artifactId(parentArtifactId)
                    .version(resolvedParentVersion)
                    .build();

            if (!parentGav.compact().equals(projectGav.compact())) {
                deps.add(MavenProject.Dependency.builder()
                        .gav(parentGav)
                        .scope("parent")
                        .optional(null)
                        .build());
            }
        }

        // Parse dependencies
        NodeList depNodes = (NodeList) xPath.evaluate(
                "/project/dependencies/dependency", doc, XPathConstants.NODESET);

        for (int i = 0; i < depNodes.getLength(); i++) {
            Element depEl = (Element) depNodes.item(i);

            String depGroupId = getChildText(depEl, "groupId");
            String depArtifactId = getChildText(depEl, "artifactId");
            String depVersion = getChildText(depEl, "version");
            String depScope = getChildText(depEl, "scope");
            String depOptionalStr = getChildText(depEl, "optional");

            if (depGroupId == null || depArtifactId == null) {
                continue;
            }

            Boolean depOptional = parseBoolean(depOptionalStr);
            String resolvedVersion = normalizeVersion(depVersion, props);

            deps.add(MavenProject.Dependency.builder()
                    .gav(GAV.builder()
                            .groupId(depGroupId)
                            .artifactId(depArtifactId)
                            .version(resolvedVersion)
                            .build())
                    .scope(depScope)
                    .optional(depOptional)
                    .build());
        }

        return MavenProject.builder()
                .project(projectGav)
                .dependencies(deps)
                .build();
    }

    private String getTextContent(Document doc, String xpath, XPath xPath) throws Exception {
        String result = (String) xPath.evaluate(xpath, doc, XPathConstants.STRING);
        return result != null && !result.trim().isEmpty() ? result.trim() : null;
    }

    private String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String text = nodes.item(0).getTextContent();
            return text != null && !text.trim().isEmpty() ? text.trim() : null;
        }
        return null;
    }

    private Map<String, String> parseProperties(Document doc, XPath xPath) throws Exception {
        Map<String, String> props = new HashMap<>();
        NodeList propNodes = (NodeList) xPath.evaluate(
                "/project/properties/*", doc, XPathConstants.NODESET);

        for (int i = 0; i < propNodes.getLength(); i++) {
            Element el = (Element) propNodes.item(i);
            String key = el.getTagName();
            String value = el.getTextContent();
            if (key != null && value != null && !value.trim().isEmpty()) {
                props.put(key, value.trim());
            }
        }
        return props;
    }

    private String resolvePlaceholders(String value, Map<String, String> props) {
        if (value == null)
            return null;

        String current = value;
        for (int i = 0; i < 5; i++) {
            Matcher matcher = PLACEHOLDER_RE.matcher(current);
            StringBuffer sb = new StringBuffer();
            boolean changed = false;

            while (matcher.find()) {
                String key = matcher.group(1);
                String replacement = props.get(key);
                if (replacement != null) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    changed = true;
                } else {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                }
            }
            matcher.appendTail(sb);
            current = sb.toString();

            if (!changed)
                break;
        }
        return current;
    }

    private String normalizeVersion(String value, Map<String, String> props) {
        if (value == null || value.isEmpty()) {
            return GAV.UNKNOWN_VERSION;
        }

        String resolved = resolvePlaceholders(value, props).trim();
        if (resolved.isEmpty()) {
            return GAV.UNKNOWN_VERSION;
        }

        // If placeholders remain, treat as unresolved
        if (PLACEHOLDER_RE.matcher(resolved).find()) {
            return GAV.UNKNOWN_VERSION;
        }

        return resolved;
    }

    private Boolean parseBoolean(String value) {
        if (value == null)
            return null;
        String v = value.trim().toLowerCase();
        if ("true".equals(v))
            return true;
        if ("false".equals(v))
            return false;
        return null;
    }
}
