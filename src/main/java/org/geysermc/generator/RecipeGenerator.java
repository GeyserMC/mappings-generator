package org.geysermc.generator;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagManager;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
        TagManager tagManager = new TagManager(registryAccess);
        registryAccess.registries().map(registryEntry -> tagManager.createLoader(resourceManager, Util.backgroundExecutor(), registryEntry))
                .map(CompletableFuture::join).forEach(loadResult -> ReloadableServerResources.updateRegistryTags(registryAccess, loadResult));
        REGISTRY_ACCESS = registryAccess;

        final List<DyeItem> allDyes = BuiltInRegistries.ITEM
                .stream()
                .filter(item -> item instanceof DyeItem)
                .map(item -> (DyeItem) item)
                .toList();
        final Map<String, MappedRecipes> recipes = new HashMap<>();
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
                validateAndAdd(shulkerRecipes, shulkerTest, item, dyeItem);
            }
        }
        recipes.put("shulker_boxes", new MappedRecipes(CraftingDataType.SHULKER_BOX, shulkerRecipes));

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
            validateAndAddWithShape(tippedArrowRecipes, tippedArrowTest, new ItemStack[]{
                    arrow, arrow, arrow,
                    arrow, potionStack, arrow,
                    arrow, arrow, arrow
            });
        });
        recipes.put("tipped_arrows", new MappedRecipes(CraftingDataType.SHAPED, tippedArrowRecipes));

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

    private static void validateAndAdd(final List<GeyserRecipe> recipes, CustomRecipe recipe, Item... inputItems) {
        List<ItemStack> inputItemStacks = createItemList(inputItems);
        var craftingContainer = new MockCraftingContainer(inputItemStacks);
        boolean matches = recipe.matches(craftingContainer, null);
        if (!matches) {
            System.out.println("Recipe does not match! Look into this...");
            return;
        }

        final ItemStack result = recipe.assemble(craftingContainer, null);
        recipes.add(new GeyserRecipe(result, Arrays.stream(inputItems).map(ItemStack::new).toList()));
    }

    private static void validateAndAddWithShape(final List<GeyserRecipe> recipes, CustomRecipe recipe, ItemStack... inputItems) {
        var craftingContainer = new MockCraftingContainer(List.of(inputItems));
        boolean matches = recipe.matches(craftingContainer, null);
        if (!matches) {
            System.out.println("Recipe does not match! Look into this...");
            return;
        }

        final ItemStack result = recipe.assemble(craftingContainer, null);
        recipes.add(new GeyserRecipe(result, Arrays.stream(inputItems).distinct().toList(), List.of("AAA", "ABA", "AAA")));
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
                    ExtraCodecs.POSITIVE_INT.fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
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

    private record GeyserRecipe(ItemStack output, List<ItemStack> inputs, List<String> shape) {
        static final Codec<GeyserRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                STACK_CODEC.fieldOf("output").forGetter(GeyserRecipe::output),
                Codec.list(STACK_CODEC).fieldOf("inputs").forGetter(GeyserRecipe::inputs),
                Codec.list(Codec.STRING).optionalFieldOf("shape", null).forGetter(GeyserRecipe::shape)
        ).apply(instance, GeyserRecipe::new));

        public GeyserRecipe(ItemStack output, List<ItemStack> inputs) {
            this(output, inputs, null);
        }
    }

    private record MappedRecipes(CraftingDataType bedrockRecipeType, List<GeyserRecipe> recipes) {
        static final Codec<MappedRecipes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("bedrock_recipe_type")
                        .xmap(CraftingDataType::valueOf, CraftingDataType::toString)
                        .forGetter(MappedRecipes::bedrockRecipeType),
                Codec.list(GeyserRecipe.CODEC).fieldOf("recipes").forGetter(MappedRecipes::recipes)
        ).apply(instance, MappedRecipes::new));
    }

    /**
     * This is what will serialize into the NBT.
     */
    private static final Codec<Map<String, MappedRecipes>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, MappedRecipes.CODEC);

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
