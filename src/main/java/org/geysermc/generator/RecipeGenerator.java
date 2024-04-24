package org.geysermc.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShulkerBoxColoring;
import net.minecraft.world.item.crafting.TippedArrowRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.CraftingDataType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class RecipeGenerator {
    private static final Codec<Map<String, List<GeyserRecipe>>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.list(GeyserRecipe.CODEC));

    public static void generate() {
        final Path mappings = Path.of("mappings");
        if (!Files.exists(mappings)) {
            System.out.println("Cannot create recipes! Did you clone submodules?");
            return;
        }
        final List<DyeItem> allDyes = BuiltInRegistries.ITEM
                .stream()
                .filter(item -> item instanceof DyeItem)
                .map(item -> (DyeItem) item)
                .toList();
        final Map<String, List<GeyserRecipe>> recipes = new HashMap<>();
        // Firework stars

        // Shulker boxes
        final List<Item> allShulkerBoxes = BuiltInRegistries.ITEM
                .stream()
                .filter(item -> Block.byItem(item) instanceof ShulkerBoxBlock)
                .toList();
        final List<GeyserRecipe> shulkerRecipes = new ArrayList<>();
        final ShulkerBoxColoring shulkerTest = new ShulkerBoxColoring(null);
        for (Item item : allShulkerBoxes) {
            for (DyeItem dyeItem : allDyes) {
                validateAndAdd(shulkerRecipes, CraftingDataType.SHULKER_BOX, shulkerTest, item, dyeItem);
            }
        }
        recipes.put("shulker_boxes", shulkerRecipes);

        // Firework rockets
        // Suspicious stews
        // Leather armor
        // Tipped arrows
        final List<GeyserRecipe> tippedArrowRecipes = new ArrayList<>();
        final TippedArrowRecipe tippedArrowTest = new TippedArrowRecipe(null);
        final ItemStack arrow = new ItemStack(Items.ARROW);
        BuiltInRegistries.POTION.forEach(potion -> {
            final ItemStack potionStack = PotionContents.createItemStack(Items.LINGERING_POTION, Holder.direct(potion));
            validateAndAddWithShape(tippedArrowRecipes, tippedArrowTest, new ItemStack[]{
                    arrow, arrow, arrow,
                    arrow, potionStack, arrow,
                    arrow, arrow, arrow
            });
        });
        recipes.put("tipped_arrows", tippedArrowRecipes);

        DataResult<Tag> result = MAP_CODEC.encodeStart(NbtOps.INSTANCE, recipes);
        result.ifSuccess(tag -> {
            try {
                NbtIo.write((CompoundTag) tag, mappings.resolve("recipes.nbt"));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).ifError(error -> {
            System.out.println("Failed to encode to NBT!");
            System.out.println(error.message());
        });
    }

    private static void validateAndAdd(final List<GeyserRecipe> recipes, CraftingDataType bedrockRecipeType, CustomRecipe recipe, Item... inputItems) {
        List<ItemStack> inputItemStacks = createItemList(inputItems);
        var craftingContainer = new MockCraftingContainer(inputItemStacks);
        boolean matches = recipe.matches(craftingContainer, null);
        if (!matches) {
            System.out.println("Recipe does not match! Look into this...");
            return;
        }

        final ItemStack result = recipe.assemble(craftingContainer, null);
        recipes.add(new GeyserRecipe(bedrockRecipeType, result, Arrays.stream(inputItems).map(ItemStack::new).toList()));
    }

    private static void validateAndAddWithShape(final List<GeyserRecipe> recipes, CustomRecipe recipe, ItemStack... inputItems) {
        var craftingContainer = new MockCraftingContainer(List.of(inputItems));
        boolean matches = recipe.matches(craftingContainer, null);
        if (!matches) {
            System.out.println("Recipe does not match! Look into this...");
            return;
        }

        final ItemStack result = recipe.assemble(craftingContainer, null);
        recipes.add(new GeyserRecipe(CraftingDataType.SHAPED, result, List.of(inputItems), List.of("AAA", "ABA", "AAA")));
    }

    private static List<ItemStack> createItemList(Item... items) {
        ItemStack[] stacks = new ItemStack[9];
        Arrays.fill(stacks, ItemStack.EMPTY);
        for (int i = 0; i < items.length; i++) {
            stacks[i] = new ItemStack(items[i]);
        }
        return List.of(stacks);
    }

    private record GeyserRecipe(CraftingDataType bedrockRecipeType, ItemStack output, List<ItemStack> inputs, List<String> shape) {
        static final Codec<GeyserRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("bedrock_recipe_type")
                        .xmap(CraftingDataType::valueOf, CraftingDataType::toString)
                        .forGetter(GeyserRecipe::bedrockRecipeType),
                ItemStack.CODEC.fieldOf("output").forGetter(GeyserRecipe::output),
                Codec.list(ItemStack.CODEC).fieldOf("inputs").forGetter(GeyserRecipe::inputs),
                Codec.list(Codec.STRING).optionalFieldOf("shape", null).forGetter(GeyserRecipe::shape)
        ).apply(instance, GeyserRecipe::new));

        public GeyserRecipe(CraftingDataType bedrockRecipeType, ItemStack output, List<ItemStack> inputs) {
            this(bedrockRecipeType, output, inputs, null);
        }
    }

    private record MockCraftingContainer(List<ItemStack> items) implements CraftingContainer {

        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }

        @Override
        public List<ItemStack> getItems() {
            return items;
        }

        @Override
        public int getContainerSize() {
            return 9;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public ItemStack getItem(int i) {
            return items.get(i);
        }

        @Override
        public ItemStack removeItem(int i, int i1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ItemStack removeItemNoUpdate(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setItem(int i, ItemStack itemStack) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setChanged() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean stillValid(Player player) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearContent() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void fillStackedContents(StackedContents stackedContents) {
            throw new UnsupportedOperationException();
        }
    }
}
