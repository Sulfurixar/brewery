package com.zener.brewery.recipes.cauldron;

import com.zener.brewery.entity.CauldronBlockEntity;
import com.zener.brewery.matcher.CauldronMatcher;
import com.zener.brewery.recipes.RecipeTypesRegistry;
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

public class CauldronRecipe implements Recipe<Inventory> {

    public static final byte INPUT_SLOTS = 64;

    @Getter private final Identifier id;
    @Getter private final String group;
    @Getter private final DefaultedList<NbtIngredient> nbtIngredients;
    @Getter private final ItemStack output;
    @Getter private final int cook_time;
    @Getter private final byte temperature;
    @Getter @Nullable private final DefaultedList<FailedOutput> failedOutputs;
    @Getter private final byte difficulty;

    public CauldronRecipe(Identifier id, String group, DefaultedList<NbtIngredient> nbtIngredients, ItemStack output, int cook_time, byte temperature, @Nullable DefaultedList<FailedOutput> failedOutputs, byte difficulty) {
        this.id = id; this.group = group;
        this.nbtIngredients = nbtIngredients; this.difficulty = difficulty;
        this.output = output; this.cook_time = cook_time; 
        this.temperature = temperature; this.failedOutputs = failedOutputs;
    }

    public boolean matches(Inventory inv, World world) {
        return false;
    }

    public CauldronMatcher advancedMatches(CauldronBlockEntity cauldron) {
        return new CauldronMatcher(cauldron, this);
    }

    @Override
    public ItemStack craft(Inventory inv) {
        ItemStack ret = new ItemStack(output.getItem(), output.getCount());
        ret.setNbt(output.getOrCreateNbt());
        return ret;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= nbtIngredients.size();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeTypesRegistry.CAULDRON_RECIPE_SERIALIZER.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeTypesRegistry.CAULDRON_RECIPE_SERIALIZER.type();
    }

    @Override
    public boolean isEmpty() {
        DefaultedList<NbtIngredient> defaultedList = this.getNbtIngredients();
        return defaultedList.isEmpty() || defaultedList.stream().anyMatch(ingredient -> ingredient.getMatchingStacks().length == 0);
    }
    
}
