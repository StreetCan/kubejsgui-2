package com.streetkube.kubejsgui.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Scans both template storage roots and merges them into a single folder tree, tracking
 * the source (universal/instance/mixed) of each node for the web UI's display icons.
 */
public final class TemplateScanner {

    private TemplateScanner() {
    }

    /** Root JSON node: {@code { "tree": [ ... ] }}. */
    public static TreeResult scan() {
        FolderBuilder root = new FolderBuilder("");
        collect(TemplateStorage.universalRoot(), "universal", root);
        collect(TemplateStorage.instanceRoot(), "instance", root);
        return new TreeResult(root.toChildNodes());
    }

    private static void collect(Path rootDir, String source, FolderBuilder root) {
        if (!Files.isDirectory(rootDir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(rootDir)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(TemplateStorage.FILE_EXT))
                    .forEach(file -> addFile(rootDir, file, source, root));
        } catch (IOException e) {
            // A missing/unreadable root is non-fatal - just contributes nothing.
        }
    }

    private static void addFile(Path rootDir, Path file, String source, FolderBuilder root) {
        Path relative = rootDir.relativize(file);
        FolderBuilder folder = root;
        for (int i = 0; i < relative.getNameCount() - 1; i++) {
            folder = folder.child(relative.getName(i).toString());
        }
        folder.markSource(source);

        String filename = file.getFileName().toString();
        String name = filename.substring(0, filename.length() - TemplateStorage.FILE_EXT.length());

        Node node = new Node();
        node.type = "file";
        node.name = name;
        node.filename = filename;
        node.path = file.toAbsolutePath().toString().replace('\\', '/');
        node.source = source;
        folder.files.add(node);
    }

    /** Output node serialized to JSON via Gson. */
    public static final class Node {
        public String type;
        public String name;
        public String source;
        public String filename;
        public String path;
        public List<Node> children;
    }

    public static final class TreeResult {
        public final List<Node> tree;

        TreeResult(List<Node> tree) {
            this.tree = tree;
        }
    }

    /** Mutable builder for merging folders from both roots. */
    private static final class FolderBuilder {
        final String name;
        final Map<String, FolderBuilder> subfolders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final List<Node> files = new ArrayList<>();
        final Map<String, Boolean> sources = new LinkedHashMap<>();

        FolderBuilder(String name) {
            this.name = name;
        }

        FolderBuilder child(String childName) {
            return subfolders.computeIfAbsent(childName, FolderBuilder::new);
        }

        void markSource(String source) {
            sources.put(source, Boolean.TRUE);
        }

        List<Node> toChildNodes() {
            List<Node> out = new ArrayList<>();
            for (FolderBuilder sub : subfolders.values()) {
                Node folderNode = new Node();
                folderNode.type = "folder";
                folderNode.name = sub.name;
                folderNode.source = sub.resolvedSourceDeep();
                folderNode.children = sub.toChildNodes();
                out.add(folderNode);
            }
            out.addAll(files);
            return out;
        }

        String resolvedSourceDeep() {
            boolean uni = sources.containsKey("universal");
            boolean inst = sources.containsKey("instance");
            for (FolderBuilder sub : subfolders.values()) {
                String s = sub.resolvedSourceDeep();
                if (s.equals("mixed")) {
                    return "mixed";
                }
                uni |= s.equals("universal");
                inst |= s.equals("instance");
            }
            for (Node file : files) {
                uni |= "universal".equals(file.source);
                inst |= "instance".equals(file.source);
            }
            if (uni && inst) {
                return "mixed";
            }
            return uni ? "universal" : "instance";
        }
    }
}
