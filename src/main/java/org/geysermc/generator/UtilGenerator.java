package org.geysermc.generator;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class UtilGenerator {

    public static void generate() {
        try {
            JsonObject util = new JsonObject();

            JsonArray gameMasterBlocks = new JsonArray();
            for (Block block : BuiltInRegistries.BLOCK) {
                if (block instanceof GameMasterBlock) {
                    gameMasterBlocks.add(BuiltInRegistries.BLOCK.getKey(block).toString());
                }
            }

            Field dangerousBlockEntitiesField = BlockEntityType.class.getDeclaredField("OP_ONLY_CUSTOM_DATA");
            dangerousBlockEntitiesField.setAccessible(true);
            Set<BlockEntityType<?>> dangerousBlockEntityTypes = (Set<BlockEntityType<?>>) dangerousBlockEntitiesField.get(null);

            Field dangerousEntitiesField = EntityType.class.getDeclaredField("OP_ONLY_CUSTOM_DATA");
            dangerousEntitiesField.setAccessible(true);
            Set<EntityType<?>> dangerousEntityTypes = (Set<EntityType<?>>) dangerousEntitiesField.get(null);

            JsonArray dangerousBlockEntities = new JsonArray();
            for (BlockEntityType<?> entityType : dangerousBlockEntityTypes) {
                dangerousBlockEntities.add(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entityType).toString());
            }

            JsonArray dangerousEntities = new JsonArray();
            for (EntityType<?> entityType : dangerousEntityTypes) {
                dangerousEntities.add(BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
            }

            util.add("game_master_blocks", gameMasterBlocks);
            util.add("dangerous_block_entities", dangerousBlockEntities);
            util.add("dangerous_entities", dangerousEntities);

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            Files.writeString(Path.of("mappings/util.json"), builder.create().toJson(util));
        } catch (NoSuchFieldException | IllegalAccessException | IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Util.initialize();
        generate();
    }
}
