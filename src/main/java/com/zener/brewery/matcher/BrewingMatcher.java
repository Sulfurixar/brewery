package com.zener.brewery.matcher;

import com.zener.brewery.recipes.brewing.BrewingRecipe;

import lombok.Getter;
import net.minecraft.item.ItemStack;

public class BrewingMatcher {

    @Getter private final Result result = new Result();

    public class Result implements MatchResult {
        public boolean isResult = false;
    }
    
    public BrewingMatcher(BrewingRecipe recipe, ItemStack ingredient, ItemStack catalyst) {
        if (recipe.getNbtIngredient().test(ingredient) == false) return;
        if (recipe.getCatalyst().test(catalyst) == false) return;

        result.isResult = true;
    }

}
