package com.streetkube.kubejsgui.mixin;

import com.streetkube.kubejsgui.block.TaggedChestAccessor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements TaggedChestAccessor {

    @Unique
    private boolean streetskubegui$tagged = false;

    @Override
    public boolean streetskubegui$isTagged() {
        return streetskubegui$tagged;
    }

    @Override
    public void streetskubegui$setTagged(boolean tagged) {
        this.streetskubegui$tagged = tagged;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void streetskubegui$saveAdditional(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (streetskubegui$tagged) {
            tag.putBoolean("streetskubegui_tagged", true);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void streetskubegui$loadAdditional(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        streetskubegui$tagged = tag.getBoolean("streetskubegui_tagged");
    }
}
