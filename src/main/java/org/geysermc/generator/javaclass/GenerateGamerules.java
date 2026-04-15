package org.geysermc.generator.javaclass;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.DeprecatedTranslationsInfo;
import net.minecraft.world.level.gamerules.GameRule;
import org.geysermc.generator.Util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.geysermc.generator.javaclass.FieldConstructor.wrap;

public class GenerateGamerules {

    static void main() {
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

            Object defaultValue = gameRule.defaultValue();
            String classType, geyserType;
            if (defaultValue instanceof Boolean) {
                classType = "Boolean";
                geyserType = "Bool";
            } else if (defaultValue instanceof Integer) {
                classType = "Integer";
                geyserType = "Int";
            } else {
                throw new RuntimeException("Unknown default value type: " + defaultValue.getClass());
            }

            FieldConstructor constructor = new FieldConstructor("GameRule", classType);
            constructor.declareFieldName(gameRule.getIdentifier().getPath().toUpperCase(Locale.ROOT));

            constructor.declareClassName("GameRule." + geyserType);
            constructor.addParameter(wrap(gameRule.getDescriptionId()));
            constructor.addParameter(wrap(translationString));

            // also add min/max for integer gamerules
            if (gameRule.argument() instanceof IntegerArgumentType integerArgumentType) {
                constructor.addParameter(integerArgumentType.getMinimum());

                int max = integerArgumentType.getMaximum();
                if (max == Integer.MAX_VALUE) {
                    constructor.addParameter("Integer.MAX_VALUE");
                } else {
                    constructor.addParameter(integerArgumentType.getMaximum());
                }
            }

            constructor.addFinishingParameter(defaultValue.toString());
            constructor.finish();
            System.out.println(constructor);
        }
    }

}
