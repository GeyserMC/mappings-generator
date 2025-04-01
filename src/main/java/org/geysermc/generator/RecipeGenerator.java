package org.geysermc.generator;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class RecipeGenerator {
    private static RegistryAccess REGISTRY_ACCESS;

    public static void generate() {
        final Path mappings = Path.of("mappings");
        if (!Files.exists(mappings)) {
            System.out.println("Cannot create recipes! Did you clone submodules?");
            return;
        }

        CloseableResourceManager resourceManager = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(ServerPacksSource.createVanillaPackSource()));
        RegistryAccess.Frozen registryAccess = RegistryLayer.createRegistryAccess().compositeAccess();
        TagLoader.loadTagsForExistingRegistries(resourceManager, registryAccess);
//        TagManager tagManager = new TagManager(registryAccess);
//        registryAccess.registries().map(registryEntry -> tagManager.createLoader(resourceManager, Util.backgroundExecutor(), registryEntry))
//                .map(CompletableFuture::join).forEach(loadResult -> ReloadableServerResources.updateRegistryTags(registryAccess, loadResult));
        REGISTRY_ACCESS = registryAccess;

        final List<DyeItem> allDyes = BuiltInRegistries.ITEM
                .stream()
                .filter(item -> item instanceof DyeItem)
                .map(item -> (DyeItem) item)
                .toList();
        final List<MappedRecipes> recipes = new ArrayList<>();

        // Firework stars - Bedrock sends more, but for now, let's just make sure something shows up in the recipe book.
//        final List<GeyserRecipe> fireworkRecipes = new ArrayList<>();
//        final FireworkRocketRecipe fireworkTest = new FireworkRocketRecipe(null);
//        validateAndAdd(fireworkRecipes, fireworkTest, Items.PAPER, Items.GUNPOWDER);
//        recipes.add(new MappedRecipes(fireworkTest.getSerializer(), fireworkRecipes));

        // Shulker boxes
        final List<Item> allShulkerBoxes = BuiltInRegistries.ITEM
                .stream()
                .filter(item -> Block.byItem(item) instanceof ShulkerBoxBlock)
                .toList();
        for (DyeItem dyeItem : allDyes) {
            final List<GeyserRecipe> shulkerRecipes = new ArrayList<>();
            final TransmuteRecipe shulkerTest = new TransmuteRecipe(
                    null,
                    null,
                    Ingredient.of(dyeItem),
                    Ingredient.of(allShulkerBoxes.stream()),
                    new TransmuteResult(ShulkerBoxBlock.getBlockByColor(dyeItem.getDyeColor()).asItem()));
            for (Item item : allShulkerBoxes) {
                validateAndAdd(shulkerRecipes, shulkerTest, item, dyeItem);
            }
            recipes.add(new MappedRecipes(shulkerTest.getSerializer(), shulkerRecipes));
        }

        // Firework rockets
        // Suspicious stews
        // Leather armor
        // Tipped arrows
        final List<GeyserRecipe> tippedArrowRecipes = new ArrayList<>();
        final TippedArrowRecipe tippedArrowTest = new TippedArrowRecipe(null);
        final ItemStack arrow = new ItemStack(Items.ARROW);
        BuiltInRegistries.POTION.forEach(potion -> {
            final ItemStack potionStack = PotionContents.createItemStack(Items.LINGERING_POTION, Holder.direct(potion));
            // Still hardcoded to heck! So don't do this in the future. :)
            validateAndAddWithShape(tippedArrowRecipes, tippedArrowTest,
                    arrow, arrow, arrow,
                    arrow, potionStack, arrow,
                    arrow, arrow, arrow);
        });
        recipes.add(new MappedRecipes(tippedArrowTest.getSerializer(), tippedArrowRecipes));

        DataResult<Tag> result = MappedRecipes.LIST_CODEC.encodeStart(NbtOps.INSTANCE, recipes);
        result.ifSuccess(tag -> {
            try {
                CompoundTag root = new CompoundTag();
                root.put("recipes", tag);
                NbtIo.write(root, mappings.resolve("recipes.nbt"));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).ifError(error -> {
            System.out.println("Failed to encode to NBT!");
            System.out.println(error.message());
        });
    }

    private static void validateAndAdd(final List<GeyserRecipe> recipes, CraftingRecipe recipe, Item... inputItems) {
        List<ItemStack> inputItemStacks = createItemList(inputItems);
        var craftingContainer = createCraftingInput(inputItemStacks);
        boolean matches = recipe.matches(craftingContainer, null);
        if (!matches) {
            System.out.println("Recipe does not match! Look into this...");
            return;
        }

        final ItemStack result = recipe.assemble(craftingContainer, null);
        recipes.add(new GeyserRecipe(result, Arrays.stream(inputItems).map(ItemStack::new).toList()));
    }

    private static void validateAndAddWithShape(final List<GeyserRecipe> recipes, CustomRecipe recipe, ItemStack... inputItems) {
        var craftingContainer = createCraftingInput(List.of(inputItems));
        boolean matches = recipe.matches(craftingContainer, null);
        if (!matches) {
            System.out.println("Recipe does not match! Look into this...");
            return;
        }

        final ItemStack result = recipe.assemble(craftingContainer, null);
        recipes.add(new GeyserRecipe(result, Arrays.stream(inputItems).distinct().toList(), List.of(IntList.of(0, 0, 0), IntList.of(0, 1, 0), IntList.of(0, 0, 0))));
    }

    private static List<ItemStack> createItemList(Item... items) {
        ItemStack[] stacks = new ItemStack[9];
        Arrays.fill(stacks, ItemStack.EMPTY);
        for (int i = 0; i < items.length; i++) {
            stacks[i] = new ItemStack(items[i]);
        }
        return List.of(stacks);
    }

    // Map ItemStacks to integer IDs and a deserializable component format
    static final Codec<ItemStack> STACK_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ExtraCodecs.POSITIVE_INT.fieldOf("id").xmap(Item::byId, Item::getId).forGetter(ItemStack::getItem),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(ItemStack::getCount),
                    Codec.STRING.optionalFieldOf("components", null)
                            .xmap(string -> DataComponentPatch.EMPTY, // Unneeded on this end of the cycle. :)
                                    components -> {
                                        if (components.isEmpty()) {
                                            return null;
                                        }
                                        // Geyser doesn't have a way to deserialize DataComponents from NBT
                                        ByteBuf byteBuf = Unpooled.buffer();
                                        var buf = new RegistryFriendlyByteBuf(byteBuf, REGISTRY_ACCESS);
                                        DataComponentPatch.STREAM_CODEC.encode(buf, components);
                                        byte[] bytes = new byte[byteBuf.readableBytes()];
                                        byteBuf.readBytes(bytes);
                                        return Base64.getEncoder().encodeToString(bytes);
                                    })
                            .forGetter(ItemStack::getComponentsPatch)
            ).apply(instance, (id, count, components) -> new ItemStack(Holder.direct(id), count, components)));

    private record GeyserRecipe(ItemStack output, List<ItemStack> inputs, List<List<Integer>> shape) {
        static final Codec<GeyserRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                STACK_CODEC.fieldOf("output").forGetter(GeyserRecipe::output),
                Codec.list(STACK_CODEC).fieldOf("inputs").forGetter(GeyserRecipe::inputs),
                Codec.list(Codec.list(Codec.INT)).optionalFieldOf("shape", null).forGetter(GeyserRecipe::shape)
        ).apply(instance, GeyserRecipe::new));

        public GeyserRecipe(ItemStack output, List<ItemStack> inputs) {
            this(output, inputs, null);
        }
    }

    /**
     * This will be serialized into the NBT.
     */
    private record MappedRecipes(RecipeSerializer serializer, List<GeyserRecipe> recipes) {
        static final Codec<MappedRecipes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("recipe_type")
                        .xmap(BuiltInRegistries.RECIPE_SERIALIZER::byId, BuiltInRegistries.RECIPE_SERIALIZER::getId)
                        .forGetter(MappedRecipes::serializer),
                Codec.list(GeyserRecipe.CODEC).fieldOf("recipes").forGetter(MappedRecipes::recipes)
        ).apply(instance, MappedRecipes::new));
        static final Codec<List<MappedRecipes>> LIST_CODEC = Codec.list(CODEC);
    }

    private static CraftingInput createCraftingInput(List<ItemStack> items) {
        return CraftingInput.of(3, 3, items);
    }
}
