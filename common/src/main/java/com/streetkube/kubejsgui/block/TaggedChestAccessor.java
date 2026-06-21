package com.streetkube.kubejsgui.block;

/**
 * Implemented on {@code BlockEntity} via mixin so the "tagged as script palette" flag
 * round-trips through {@code saveAdditional}/{@code loadAdditional} as block entity NBT.
 */
public interface TaggedChestAccessor {
    boolean streetskubegui$isTagged();

    void streetskubegui$setTagged(boolean tagged);
}
