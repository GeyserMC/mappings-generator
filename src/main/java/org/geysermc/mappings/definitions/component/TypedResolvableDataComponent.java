package org.geysermc.mappings.definitions.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.codec.HolderSetCodec;
import net.minecraft.core.registries.codec.RegistryFileCodec;
import net.minecraft.core.registries.codec.RegistryFixedCodec;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.InstrumentComponent;

import java.util.ArrayList;
import java.util.List;

public record TypedResolvableDataComponent(DataComponentType<?> type, ResolvableDataComponent value) {
    public static final Codec<TypedResolvableDataComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    DataComponentType.PERSISTENT_CODEC.fieldOf("component").forGetter(TypedResolvableDataComponent::type),
                    ResolvableDataComponent.MAP_CODEC.forGetter(TypedResolvableDataComponent::value)
            ).apply(instance, TypedResolvableDataComponent::new)
    );
    // Not translating enchantments/stored enchantments: as far as I know, there are no items with enchantments built-in
    // Not translating trims: as far as I know, there are no items with trims built-in
    // Not translating banner patterns: all usages in Items have no layers?
    private static final List<ResolvableDataComponentType<?>> CUSTOM_RESOLVABLE_COMPONENTS = List.of(
            ResolvableDataComponentType.ofHolderSet(DataComponents.DAMAGE_RESISTANT, DamageResistant::types),
            ResolvableDataComponentType.ofHolder(DataComponents.INSTRUMENT, InstrumentComponent::instrument, true),
            ResolvableDataComponentType.ofHolder(DataComponents.JUKEBOX_PLAYABLE, JukeboxPlayable::song, true),
            // Manually adding these 2: they are not detected automatically, because they use .cacheEncoding, which wraps their codec and makes it unable to detect
            // (until we have mixins)
            ResolvableDataComponentType.ofHolder(DataComponents.PROVIDES_TRIM_MATERIAL),
            ResolvableDataComponentType.ofHolderSet(DataComponents.PROVIDES_BANNER_PATTERNS)
    );

    public static List<List<TypedResolvableDataComponent>> collectAll() {
        List<ResolvableDataComponentType<?>> resolvableDataComponentTypes = new ArrayList<>(CUSTOM_RESOLVABLE_COMPONENTS);
        for (DataComponentType<?> componentType : BuiltInRegistries.DATA_COMPONENT_TYPE) {
            Codec<?> valueCodec = componentType.codec();
            if (valueCodec instanceof RegistryFixedCodec<?> || valueCodec instanceof RegistryFileCodec<?>) {
                // These usually have Holders as values - if not then it'll just throw, and we can add an exception for it
                resolvableDataComponentTypes.add(ResolvableDataComponentType.ofHolder((DataComponentType) componentType));
            } else if (valueCodec instanceof HolderSetCodec<?>) {
                // Same as above
                resolvableDataComponentTypes.add(ResolvableDataComponentType.ofHolderSet((DataComponentType) componentType));
            }
        }

        List<List<TypedResolvableDataComponent>> allResolvableDataComponents = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            DataComponentMap components = item.components();
            List<TypedResolvableDataComponent> resolvableDataComponents = new ArrayList<>();
            for (ResolvableDataComponentType<?> resolvable : resolvableDataComponentTypes) {
                resolvable.get(components).ifPresent(resolvableDataComponents::add);
            }
            allResolvableDataComponents.add(resolvableDataComponents);
        });

        return allResolvableDataComponents;
    }
}
