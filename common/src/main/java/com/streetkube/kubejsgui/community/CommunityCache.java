package com.streetkube.kubejsgui.community;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * In-memory merged entry list from all active repos, refreshed on demand. A stale cache is
 * served while a refresh runs (refresh swaps the lists atomically at the end).
 */
public final class CommunityCache {

    private static volatile List<CommunityEntry> entries = List.of();
    private static volatile List<RepoStatus> statuses = List.of();
    private static volatile boolean loaded = false;

    private CommunityCache() {
    }

    public static final class RepoStatus {
        public String url;
        public boolean enabled;
        public boolean isDefault;
        public String repoName;
        public int entryCount;
        public String error;
    }

    public static List<CommunityEntry> getEntries() {
        return entries;
    }

    public static List<RepoStatus> getStatuses() {
        return statuses;
    }

    public static List<RepoStatus> getErrors() {
        List<RepoStatus> out = new ArrayList<>();
        for (RepoStatus s : statuses) {
            if (s.error != null) {
                out.add(s);
            }
        }
        return out;
    }

    public static void ensureLoaded() {
        if (!loaded) {
            refresh();
        }
    }

    public static void refreshAsync() {
        CompletableFuture.runAsync(CommunityCache::refresh);
    }

    public static synchronized void refresh() {
        List<RepoConfig.Repo> repos = RepoConfig.getAll();
        Map<String, CompletableFuture<RepoFetcher.Result>> futures = new LinkedHashMap<>();
        for (RepoConfig.Repo repo : repos) {
            if (repo.enabled()) {
                futures.put(repo.url(), RepoFetcher.fetchAsync(repo.url()));
            }
        }

        List<CommunityEntry> merged = new ArrayList<>();
        List<RepoStatus> sts = new ArrayList<>();
        for (RepoConfig.Repo repo : repos) {
            RepoStatus s = new RepoStatus();
            s.url = repo.url();
            s.enabled = repo.enabled();
            s.isDefault = repo.isDefault();
            if (repo.enabled()) {
                try {
                    RepoFetcher.Result r = futures.get(repo.url()).get(15, TimeUnit.SECONDS);
                    if (r.error != null) {
                        s.error = r.error;
                    } else {
                        s.repoName = r.index.repoName;
                        String base = baseUrl(repo.url());
                        int count = 0;
                        for (CommunityEntry e : r.index.entries) {
                            if (e == null || e.path == null) {
                                continue;
                            }
                            e.repoName = r.index.repoName;
                            e.repoUrl = repo.url();
                            e.fileUrl = base + e.path;
                            merged.add(e);
                            count++;
                        }
                        s.entryCount = count;
                    }
                } catch (Exception ex) {
                    s.error = "Failed to fetch: " + ex.getMessage();
                }
            }
            sts.add(s);
        }

        entries = merged;
        statuses = sts;
        loaded = true;
    }

    /** Hosts of all active repos, used by the installer to reject off-repo file URLs. */
    public static Set<String> activeHosts() {
        Set<String> hosts = new LinkedHashSet<>();
        for (String url : RepoConfig.getActiveUrls()) {
            try {
                String host = URI.create(url).getHost();
                if (host != null) {
                    hosts.add(host);
                }
            } catch (RuntimeException ignored) {
                // skip malformed
            }
        }
        return hosts;
    }

    static String baseUrl(String url) {
        int i = url.lastIndexOf('/');
        return i >= 0 ? url.substring(0, i + 1) : url;
    }
}
