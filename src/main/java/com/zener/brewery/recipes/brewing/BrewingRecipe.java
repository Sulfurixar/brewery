package com.zener.brewery.recipes.brewing;

import com.zener.brewery.matcher.BrewingMatcher;
import com.zener.brewery.recipes.RecipeTypesRegistry;
import com.zener.brewery.recipes.cauldron.FailedOutput;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredient;

import blue.endless.jankson.annotation.Nullable;
import lombok.Getter;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class BrewingRecipe implements Recipe<Inventory> {

    public static final byte INPUT_SLOTS = 1;

    @Getter private final Identifier id;
    @Getter private final String group;
    @Getter private final NbtIngredient nbtIngredient;
    @Getter private final NbtIngredient catalyst;
    @Getter private final ItemStack output;
    @Getter @Nullable private final DefaultedList<FailedOutput> failedOutputs;

    public BrewingRecipe(Identifier id, String group, NbtIngredient nbtIngredient, NbtIngredient catalyst, ItemStack output, @Nullable DefaultedList<FailedOutput> failedOutputs) {
        this.id = id; this.group = group;
        this.nbtIngredient = nbtIngredient; this.catalyst = catalyst;
        this.output = output; this.failedOutputs = failedOutputs;
    }

    public boolean matches(Inventory inv, World world) {
        return false;
    }

    public BrewingMatcher advancedMatches(ItemStack ingredient, ItemStack catalyst) {
        return new BrewingMatcher(this, ingredient, catalyst);
    }

    @Override
    public ItemStack craft(Inventory inv) {
        ItemStack ret = new ItemStack(output.getItem(), output.getCount());
        ret.setNbt(output.getOrCreateNbt());
        return ret;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeTypesRegistry.BREWING_RECIPE_SERIALIZER.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeTypesRegistry.BREWING_RECIPE_SERIALIZER.type();
    }

    @Override
    public boolean isEmpty() {
        return (nbtIngredient.getMatchingStacks().length == 1 && catalyst.getMatchingStacks().length == 1);
    }
    
}
