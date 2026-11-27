package org.geysermc.mappings.transformer.instance;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantments;
import org.geysermc.mappings.transformer.Transformer;
import org.geysermc.mappings.util.MappingsUtil;

public final class EnchantmentMappings {
    public static final Transformer<Identifier, String> INSTANCE = Transformer.of(builder -> builder
            .mapInput(enchantment -> ResourceKey.create(Registries.ENCHANTMENT, enchantment))

            // Sweeping Edge doesn't exist on bedrock :(
            .transform(Enchantments.BINDING_CURSE, "CurseOfBinding")
            .transform(Enchantments.VANISHING_CURSE, "CurseOfVanishing")

            // Map all other enchantments by simply converting Java name to pascal case
            .finish(identifier -> MappingsUtil.snakeToPascalCase(identifier.getPath())));

    private EnchantmentMappings() {}
}
