package br.com.quantumindustries.recipe;

import br.com.quantumindustries.research.ResearchCatalog;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** A datapack process, shared by the server machine and the research notebook. */
public record MechanicalProcessingRecipe(List<Item> inputs, ItemStackTemplate output, int ticks,
                                         int mechanicalLoad, String research) implements Recipe<SingleRecipeInput> {
    private static final Codec<List<Item>> INPUTS = BuiltInRegistries.ITEM.byNameCodec().listOf()
            .validate(items -> items.isEmpty() || items.size() > 64
                    ? DataResult.error(() -> "Expected 1..64 input alternatives") : DataResult.success(items));
    private static final Codec<String> RESEARCH = Codec.STRING.validate(key -> ResearchCatalog.find(key) == null
            ? DataResult.error(() -> "Unknown research " + key) : DataResult.success(key));
    public static final MapCodec<MechanicalProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            INPUTS.fieldOf("inputs").forGetter(MechanicalProcessingRecipe::inputs),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(MechanicalProcessingRecipe::output),
            Codec.intRange(1, 12000).fieldOf("ticks").forGetter(MechanicalProcessingRecipe::ticks),
            Codec.intRange(1, 1024).fieldOf("mechanical_load").forGetter(MechanicalProcessingRecipe::mechanicalLoad),
            RESEARCH.fieldOf("research").forGetter(MechanicalProcessingRecipe::research)
    ).apply(instance, MechanicalProcessingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MechanicalProcessingRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    public MechanicalProcessingRecipe {
        inputs = List.copyOf(inputs);
        if (inputs.isEmpty() || output.count() < 1 || output.count() > 64
                || ticks <= 0 || mechanicalLoad <= 0) throw new IllegalArgumentException("Invalid mechanical process");
    }

    public ItemStack result() { return output.create(); }
    public boolean accepts(ItemStack stack) { return inputs.stream().anyMatch(stack::is); }
    @Override public boolean matches(SingleRecipeInput input, Level level) { return accepts(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input) { return result(); }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return "quantumindustries:mechanical_sawmill"; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<MechanicalProcessingRecipe> getSerializer() { return ModRecipes.MECHANICAL_PROCESSING.get(); }
    @Override public RecipeType<MechanicalProcessingRecipe> getType() { return ModRecipes.MECHANICAL_PROCESSING_TYPE.get(); }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
