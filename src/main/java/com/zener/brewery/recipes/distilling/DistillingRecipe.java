package com.zener.brewery.recipes.distilling;

import com.zener.brewery.matcher.DistillingMatcher;
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

public class DistillingRecipe implements Recipe<Inventory> {

    public static final byte INPUT_SLOTS = 5;

    @Getter private final Identifier id;
    @Getter private final String group;
    @Getter private final NbtIngredient nbtIngredient;
    @Getter private final ItemStack output;
    @Getter @Nullable private final DefaultedList<FailedOutput> failedOutputs;
    @Getter private final byte distills;

    public DistillingRecipe(Identifier id, String group, NbtIngredient nbtIngredient, byte distills, ItemStack output, @Nullable DefaultedList<FailedOutput> failedOutputs) {
        this.id = id; this.group = group;
        this.nbtIngredient = nbtIngredient; this.distills = distills;
        this.output = output; this.failedOutputs = failedOutputs;
    }

    public boolean matches(Inventory inv, World world) {
        return false;
    }

    public DistillingMatcher advancedMatches(ItemStack ingredient) {
        return new DistillingMatcher(this, ingredient);
    }

    @Override
    public ItemStack craft(Inventory inv) {
        ItemStack ret = new ItemStack(output.getItem(), output.getCount());
        ret.setNbt(output.getOrCreateNbt());
        return ret;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height == 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeTypesRegistry.DISTILLING_RECIPE_SERIALIZER.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeTypesRegistry.DISTILLING_RECIPE_SERIALIZER.type();
    }

    @Override
    public boolean isEmpty() {
        return this.nbtIngredient != null;
    }
    
}
