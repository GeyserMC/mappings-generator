package org.geysermc.generator.javaclass;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.DeprecatedTranslationsInfo;
import net.minecraft.world.level.gamerules.GameRule;
import org.geysermc.generator.Util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GenerateGamerules {

    public static void main(String[] args) {
        Util.initialize();

        // TODO implement the deprecations parser in Geyser
        DeprecatedTranslationsInfo info = DeprecatedTranslationsInfo.loadFromDefaultResource();
        Map<String, String> reversed = new HashMap<>();
        info.renamed().forEach((key, value) -> reversed.put(value, key));

        for (int i = 0; i < BuiltInRegistries.GAME_RULE.size(); i++) {
            GameRule<?> gameRule = BuiltInRegistries.GAME_RULE.byId(i);

            String translationString = gameRule.getDescriptionId();
            if (reversed.containsKey(translationString)) {
                translationString = reversed.get(translationString);
            }

            StringBuilder builder = new StringBuilder();
            builder.append(gameRule.getIdentifier().getPath().toUpperCase(Locale.ROOT));
            builder.append("(\"");
            builder.append(translationString);
            builder.append("\", ");
            builder.append(gameRule.defaultValue());
            builder.append(")");

            if (i == BuiltInRegistries.GAME_RULE.size() - 1) {
                builder.append(";");
            } else {
                builder.append(",");
            }

            System.out.println(builder.toString());
        }
    }

}
