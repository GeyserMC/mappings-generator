package org.geysermc.mappings.transformer.instance;

import net.minecraft.core.Holder;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import org.geysermc.mappings.definitions.item.BedrockMapDecoration;
import org.geysermc.mappings.definitions.protocol.EnumConstant;
import org.geysermc.mappings.transformer.Transformer;

import java.util.Set;

public final class MapDecorationMappings {
    public static final Transformer.MaybeCreator<Set<EnumConstant>, Holder<MapDecorationType>, BedrockMapDecoration.Serialized> INSTANCE = bedrockDecorations -> Transformer.maybe(builder -> builder
            .<BedrockMapDecoration>flatMapOutput(decoration -> decoration.serialize(bedrockDecorations))
            .mark()

            // Simple renames
            .<String>mapOutput(BedrockMapDecoration::new)
            .transform(MapDecorationTypes.PLAYER, "MarkerWhite")
            .transform(MapDecorationTypes.FRAME, "MarkerSign")
            .transform(MapDecorationTypes.RED_MARKER, "MarkerRed")
            .transform(MapDecorationTypes.BLUE_MARKER, "MarkerBlue")
            .transform(MapDecorationTypes.TARGET_POINT, "TriangleRed")
            .transform(MapDecorationTypes.PLAYER_OFF_MAP, "SquareWhite")
            .transform(MapDecorationTypes.PLAYER_OFF_LIMITS, "SmallSquareWhite")
            .transform(MapDecorationTypes.RED_X, "XWhite")
            .transform(MapDecorationTypes.SWAMP_HUT, "WitchHut")
            .transform(MapDecorationTypes.ANCIENT_CITY, "BuriedAncientCity")
            .transform(MapDecorationTypes.MINESHAFT, "BuriedMineshaft")
            .transform(MapDecorationTypes.OCEAN_RUIN_WARM, "WarmOceanRuins")

            // Banners, all one icon but with different colours
            .reset()
            .<Integer>mapOutput(rgb -> new BedrockMapDecoration("SmallSquareWhite", ARGB.color(0xFF, rgb)))
            .transform(MapDecorationTypes.WHITE_BANNER, 0xFFFFFF)
            .transform(MapDecorationTypes.ORANGE_BANNER, 0xF9801D)
            .transform(MapDecorationTypes.MAGENTA_BANNER, 0xC74EBD)
            .transform(MapDecorationTypes.LIGHT_BLUE_BANNER, 0x3AB3DA)
            .transform(MapDecorationTypes.YELLOW_BANNER, 0xFED83D)
            .transform(MapDecorationTypes.LIME_BANNER, 0x80C71F)
            .transform(MapDecorationTypes.PINK_BANNER, 0xF38BAA)
            .transform(MapDecorationTypes.GRAY_BANNER, 0x474F52)
            .transform(MapDecorationTypes.LIGHT_GRAY_BANNER, 0x9D9D97)
            .transform(MapDecorationTypes.CYAN_BANNER, 0x169C9C)
            .transform(MapDecorationTypes.PURPLE_BANNER, 0x8932B8)
            .transform(MapDecorationTypes.BLUE_BANNER, 0x3C44AA)
            .transform(MapDecorationTypes.BROWN_BANNER, 0x835432)
            .transform(MapDecorationTypes.GREEN_BANNER, 0x5E7C16)
            .transform(MapDecorationTypes.RED_BANNER, 0xB02E26)
            .transform(MapDecorationTypes.BLACK_BANNER, 0x1D1D21)

            // A unique situation where the decoration doesn't exist on Bedrock, and we replace it with a black cross
            .reset()
            .transform(MapDecorationTypes.TARGET_X, new BedrockMapDecoration("XWhite", 0xFF000000))

            // Map the remaining by simply copying Java's name
            .tryAll(BedrockMapDecoration::new));

    private MapDecorationMappings() {}
}
