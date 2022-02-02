package com.zener.brewery.recipes.nbt_ingredient;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.zener.brewery.recipes.brewing.BrewingIngredient;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.World;

public class NbtIngredientManager {

    private static HashMap<NbtIngredientType<?>, Map<Identifier, NbtIngredient>> ingredients = new HashMap<NbtIngredientType<?>, Map<Identifier, NbtIngredient>>();

    public static void apply(Identifier id, NbtIngredient ingredient) {

        if (!ingredients.keySet().contains(ingredient.getType())) ingredients.put(ingredient.getType(), new HashMap<Identifier, NbtIngredient>());
        Map<Identifier, NbtIngredient> typeList = ingredients.get(ingredient.getType());
        typeList.put(id, ingredient);
        ingredients.put(ingredient.getType(), typeList);
    }

    public static <C extends PlayerEntity, T extends NbtIngredient> List<NbtIngredient> getAllMatches(NbtIngredientType<T> type, C player, World world) {
        return NbtIngredientManager.getAllOfType(type).values().stream().flatMap(
            ingredient -> Util.stream(type.match(ingredient, world, player))
            ).collect(Collectors.toList());
    }

    public static <T extends NbtIngredient> List<NbtIngredient> getAllMatches(NbtIngredientType<T> type, ItemStack itemStack) {
        return NbtIngredientManager.getAllOfType(type).values().stream().flatMap(ingredient -> Util.stream(
            type.match(ingredient, itemStack)
        )).collect(Collectors.toList());
    }

    public static <T extends BrewingIngredient> List<NbtIngredient> getAllMatches(NbtIngredientType<T> type, ItemStack itemStack, ItemStack itemStack2, World world) {
        return NbtIngredientManager.getAllOfType(type).values().stream().flatMap(ingredient -> Util.stream(
            type.match((BrewingIngredient)ingredient, itemStack, itemStack2, world)
        )).collect(Collectors.toList());
    }

    public static <T extends NbtIngredient> Map<Identifier, NbtIngredient> getAllMatches(NbtIngredientType<T> type) {
        return NbtIngredientManager.getAllOfType(type);
    }

    private static <C extends PlayerEntity, T extends NbtIngredient> Map<Identifier, NbtIngredient> getAllOfType(NbtIngredientType<T> type) {
        return ingredients.getOrDefault(type, Collections.emptyMap());
    }

    public static boolean isValidIngredient(ItemStack stack) {
        return NbtIngredientManager.isDistillingRecipeIngredient(stack);
    }

    public static boolean isDistillingRecipeIngredient(ItemStack stack) {
        Map<Identifier, NbtIngredient> distilling_recipes = NbtIngredientManager.getAllMatches(NbtIngredientType.DISTILLING);
        NbtIngredient[] recipes = distilling_recipes.values().toArray(new NbtIngredient[distilling_recipes.size()]);
        for (int i = 0; i < recipes.length; ++i) {
            if (!recipes[i].test(stack)) continue;
            return true;
        }
        return false;
    }

}
