package com.zener.brewery.recipes.brewing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.zener.brewery.Brewery;
import com.zener.brewery.matcher.BrewingMatcher.Result;
import com.zener.brewery.mixin.RecipeManagerAccessor;
import com.zener.brewery.recipes.RecipeTypesRegistry;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredient;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredientType;

import blue.endless.jankson.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.world.World;

public class BrewingIngredient extends NbtIngredient {
    
    private final static NbtIngredientType<BrewingIngredient> type = NbtIngredientType.BREWING;

    public static final BrewingIngredient EMPTY = new BrewingIngredient(new Identifier(Brewery.MOD_ID+"_empty"), Stream.empty());

    public BrewingIngredient(Identifier id, Stream<? extends Entry> entries) {
        super(id, entries, type);
    }

    public NbtIngredientType<?> getType() {
        return super.getType();
    }

    public static BrewingIngredient fromJson(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull()) {
            throw new JsonSyntaxException("Item cannot be null");
        }
        if (json.isJsonObject()) {
            StackEntry entry = (StackEntry) BrewingIngredient.entryFromJson(json.getAsJsonObject());
            
            return BrewingIngredient.ofEntries(entry.getId(), Stream.of(entry));
        }
        if (json.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            if (jsonArray.size() == 0) {
                throw new JsonSyntaxException("Item array cannot be empty, at least one item must be defined");
            }
            TagEntry entry = (TagEntry) BrewingIngredient.entryFromJson(json.getAsJsonObject());
            return BrewingIngredient.ofEntries(entry.getId(), StreamSupport.stream(jsonArray.spliterator(), false).map(jsonElement -> entryFromJson(JsonHelper.asObject(jsonElement, "item"))));
        }
        throw new JsonSyntaxException("Expected item to be object or array of objects");
    }

    private static BrewingIngredient ofEntries(Identifier id, Stream<? extends Entry> entries) {
        BrewingIngredient ingredient = new BrewingIngredient(id, entries);
        return ingredient.entries.length == 0 ? EMPTY : ingredient;
    }

    public boolean matches(ItemStack stack, ItemStack stack2, World world) {

        
        RecipeManager recipeManager = world.getRecipeManager();
        RecipeManagerAccessor accessor = (RecipeManagerAccessor) recipeManager;
        List<Result> results = new ArrayList<Result>();

        accessor.callGetAllOfType(RecipeTypesRegistry.BREWING_RECIPE_SERIALIZER.type()).forEach((id, recipe) -> {
            Result match = ((BrewingRecipe) recipe).advancedMatches(stack, stack2).getResult();
            results.add(match);
        });

        return results.stream().filter(m -> m.isResult).collect(ArrayList<Result>::new, (l, e) -> l.add(e), ArrayList::addAll).size() > 1;
    }
    
}
