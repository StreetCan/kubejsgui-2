package com.streetkube.kubejsgui.tags;

import java.util.List;

/** A single tag: its id and the (sorted) ids of its members. */
public final class TagEntry {

    public final String id;
    public final int memberCount;
    public final List<String> members;

    public TagEntry(String id, List<String> members) {
        this.id = id;
        this.members = members;
        this.memberCount = members.size();
    }
}
