package org.geysermc.mappings.mixin;

import net.fabricmc.fabric.impl.datagen.client.SoundTypeBuilderImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/// Some of Fabric's codecs are used in SoundEventAliases, and they are stricter than what Mojang allows.
@SuppressWarnings("UnstableApiUsage")
@Mixin(SoundTypeBuilderImpl.Entry.class)
public abstract class SoundTypeBuilderImplEntryMixin {

    @ModifyConstant(method = "lambda$static$0", constant = @Constant(floatValue = 1.0F, ordinal = 0))
    private static float modifyMaxVolume(float original) {
        return Float.MAX_VALUE;
    }

    @ModifyConstant(method = "lambda$static$0", constant = @Constant(floatValue = 0.5F, ordinal = 0))
    private static float modifyMinPitch(float original) {
        return Float.MIN_VALUE;
    }

    @ModifyConstant(method = "lambda$static$0", constant = @Constant(floatValue = 2.0F, ordinal = 0))
    private static float modifyMaxPitch(float original) {
        return Float.MAX_VALUE;
    }
}
