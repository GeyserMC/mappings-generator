package org.geysermc.mappings.generator;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.names.Renamers;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PotPatternMappingsGenerator extends MappingsGenerator<Map<Holder<DecoratedPotPattern>, Identifier>> {
    private final CompletableFuture<RegistryAccess> registries;

    public PotPatternMappingsGenerator(PackOutput output, CompletableFuture<RegistryAccess> registries) {
        super(output, FileType.DECORATED_POT_PATTERNS);
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(registries -> saveFile(cache, registries, BuiltInRegistries.ITEM.stream()
                .map(item -> {
                    Holder<DecoratedPotPattern> pattern = item.components().get(DataComponents.PROVIDES_POTTERY_PATTERN);
                    if (pattern == null) {
                        return null;
                    }
                    return Pair.of(pattern, Renamers.ITEMS.forType(item).apply(BuiltInRegistries.ITEM.getKey(item)));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond))));
    }

    @Override
    public String getName() {
        return "Decorated Pot Pattern Mappings Generator";
    }
}
