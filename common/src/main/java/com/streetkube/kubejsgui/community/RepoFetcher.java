package com.streetkube.kubejsgui.community;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches and parses a repo's index.json. HTTPS-only, 10s timeout, 1MB cap. Failures are
 * returned as a {@link Result} with an error message rather than thrown.
 */
public final class RepoFetcher {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private static final Gson GSON = new Gson();
    private static final int MAX_BYTES = 1_000_000;

    private RepoFetcher() {
    }

    public static final class RepoIndex {
        public String repoName;
        public String repoAuthor;
        public String repoVersion;
        public String repoDescription;
        public List<CommunityEntry> entries;
    }

    public static final class Result {
        public RepoIndex index;
        public String error;

        static Result ok(RepoIndex index) {
            Result r = new Result();
            r.index = index;
            return r;
        }

        static Result error(String error) {
            Result r = new Result();
            r.error = error;
            return r;
        }
    }

    /**
     * Converts a GitHub web URL ({@code github.com/.../blob/...} or {@code .../raw/...}) into the
     * matching {@code raw.githubusercontent.com} URL that actually serves the file. Other URLs are
     * returned trimmed and unchanged.
     */
    public static String normalizeRepoUrl(String url) {
        if (url == null) {
            return null;
        }
        String u = url.trim();
        if (u.startsWith("https://github.com/")) {
            u = u.replaceFirst("^https://github\\.com/", "https://raw.githubusercontent.com/")
                    .replaceFirst("/blob/", "/")
                    .replaceFirst("/raw/", "/");
        }
        return u;
    }

    public static CompletableFuture<Result> fetchAsync(String url) {
        url = normalizeRepoUrl(url);
        if (url == null || !url.toLowerCase().startsWith("https://")) {
            return CompletableFuture.completedFuture(Result.error("Only https:// URLs are allowed"));
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (RuntimeException e) {
            return CompletableFuture.completedFuture(Result.error("Invalid URL"));
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10)).GET().build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(RepoFetcher::parse)
                .exceptionally(ex -> Result.error("Failed to fetch: " + rootMessage(ex)));
    }

    private static Result parse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            return Result.error("HTTP " + response.statusCode());
        }
        String body = response.body();
        if (body.length() > MAX_BYTES) {
            return Result.error("index.json exceeds 1MB limit");
        }
        try {
            RepoIndex index = GSON.fromJson(body, RepoIndex.class);
            if (index == null || index.entries == null) {
                return Result.error("No 'entries' array in index.json");
            }
            return Result.ok(index);
        } catch (JsonSyntaxException e) {
            return Result.error("Invalid JSON in index.json");
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        String m = cur.getMessage();
        return m != null ? m : cur.getClass().getSimpleName();
    }
}
