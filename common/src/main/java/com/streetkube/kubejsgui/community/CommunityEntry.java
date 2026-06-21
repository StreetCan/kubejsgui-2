package com.streetkube.kubejsgui.community;

import java.util.List;

/**
 * One entry from a repo's index.json. The first block of fields is parsed straight from
 * JSON; the trailing fields are enriched by {@link CommunityCache} after fetch.
 */
public final class CommunityEntry {
    public String name;
    public String author;
    public String version;
    public String type;
    public String description;
    public String path;
    public List<String> tags;

    // Enriched after fetch:
    public String repoName;
    public String repoUrl;
    public String fileUrl;
}
