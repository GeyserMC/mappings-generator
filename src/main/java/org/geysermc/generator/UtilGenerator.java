package org.geysermc.generator;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UtilGenerator {

    public static void generate() {
        try {
            JsonObject util = new JsonObject();

            List<Identifier> gameMasterBlocks = new ArrayList<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                if (block instanceof GameMasterBlock) {
                    gameMasterBlocks.add(BuiltInRegistries.BLOCK.getKey(block));
                }
            }

            Field dangerousBlockEntitiesField = BlockEntityType.class.getDeclaredField("OP_ONLY_CUSTOM_DATA");
            dangerousBlockEntitiesField.setAccessible(true);
            Set<BlockEntityType<?>> dangerousBlockEntityTypes = (Set<BlockEntityType<?>>) dangerousBlockEntitiesField.get(null);

            Field dangerousEntitiesField = EntityType.class.getDeclaredField("OP_ONLY_CUSTOM_DATA");
            dangerousEntitiesField.setAccessible(true);
            Set<EntityType<?>> dangerousEntityTypes = (Set<EntityType<?>>) dangerousEntitiesField.get(null);

            List<Identifier> dangerousBlockEntities = new ArrayList<>();
            for (BlockEntityType<?> entityType : dangerousBlockEntityTypes) {
                dangerousBlockEntities.add(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entityType));
            }

            List<Identifier> dangerousEntities = new ArrayList<>();
            for (EntityType<?> entityType : dangerousEntityTypes) {
                dangerousEntities.add(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
            }

            util.add("game_master_blocks", sortAndToJson(gameMasterBlocks));
            util.add("dangerous_block_entities", sortAndToJson(dangerousBlockEntities));
            util.add("dangerous_entities", sortAndToJson(dangerousEntities));

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            Files.writeString(Path.of("mappings/util.json"), builder.create().toJson(util));
        } catch (NoSuchFieldException | IllegalAccessException | IOException exception) {
            exception.printStackTrace();
        }
    }

    private static JsonArray sortAndToJson(List<Identifier> list) {
        JsonArray array = new JsonArray();
        list.stream().sorted().forEach(location -> array.add(location.toString()));
        return array;
    }

    public static void main(String[] args) {
        Util.initialize();
        generate();
    }
}
