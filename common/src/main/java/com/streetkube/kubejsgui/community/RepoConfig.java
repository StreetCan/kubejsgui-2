package com.streetkube.kubejsgui.community;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.streetkube.kubejsgui.template.TemplateStorage;

/**
 * Reads/writes {@code kubejs-gui-repos.txt} in the universal config directory. Disabled
 * repos are persisted with a {@code #disabled } prefix so they survive but are skipped.
 */
public final class RepoConfig {

    public static final String DEFAULT_REPO_URL =
            "https://raw.githubusercontent.com/StreetCan/KubeJS-GUI-2-Community-Scripts/refs/heads/main/index.json";

    private static final String DISABLED_PREFIX = "#disabled ";

    private RepoConfig() {
    }

    public record Repo(String url, boolean enabled) {
        public boolean isDefault() {
            return url.equals(DEFAULT_REPO_URL);
        }
    }

    private static Path file() {
        return TemplateStorage.universalRoot().resolve("kubejs-gui-repos.txt");
    }

    public static synchronized void ensureExists() {
        Path f = file();
        if (Files.exists(f)) {
            return;
        }
        write(List.of(new Repo(DEFAULT_REPO_URL, true)));
    }

    public static synchronized List<Repo> getAll() {
        ensureExists();
        List<Repo> repos = new ArrayList<>();
        try {
            for (String raw : Files.readAllLines(file(), StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith(DISABLED_PREFIX)) {
                    String url = line.substring(DISABLED_PREFIX.length()).trim();
                    if (!url.isEmpty()) {
                        repos.add(new Repo(url, false));
                    }
                } else if (!line.startsWith("#")) {
                    repos.add(new Repo(line, true));
                }
            }
        } catch (IOException e) {
            // Treat an unreadable config as empty.
        }
        return repos;
    }

    public static List<String> getActiveUrls() {
        List<String> out = new ArrayList<>();
        for (Repo r : getAll()) {
            if (r.enabled()) {
                out.add(r.url());
            }
        }
        return out;
    }

    public static synchronized boolean add(String url) {
        List<Repo> repos = getAll();
        for (Repo r : repos) {
            if (r.url().equals(url)) {
                return false; // already present
            }
        }
        repos.add(new Repo(url, true));
        write(repos);
        return true;
    }

    public static synchronized void remove(String url) {
        List<Repo> repos = getAll();
        repos.removeIf(r -> r.url().equals(url));
        write(repos);
    }

    public static synchronized void setEnabled(String url, boolean enabled) {
        List<Repo> repos = getAll();
        for (int i = 0; i < repos.size(); i++) {
            if (repos.get(i).url().equals(url)) {
                repos.set(i, new Repo(url, enabled));
            }
        }
        write(repos);
    }

    private static void write(List<Repo> repos) {
        StringBuilder sb = new StringBuilder();
        sb.append("# KubeJS GUI 2 — Community Repositories\n");
        sb.append("# One URL per line. Lines starting with # are comments.\n");
        sb.append("# Disabled repos are prefixed with \"#disabled \".\n");
        sb.append("# WARNING: Only add repos from sources you trust.\n\n");
        for (Repo r : repos) {
            if (r.isDefault()) {
                sb.append("# Default repository — bundled with KubeJS GUI 2\n");
            }
            sb.append(r.enabled() ? r.url() : DISABLED_PREFIX + r.url()).append('\n');
        }
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Non-fatal: a failed write just means the change isn't persisted.
        }
    }
}
