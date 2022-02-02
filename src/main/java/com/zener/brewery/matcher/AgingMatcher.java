package com.zener.brewery.matcher;

import com.zener.brewery.recipes.aging.AgingRecipe;

import lombok.Getter;
import net.minecraft.item.ItemStack;

public class AgingMatcher {

    @Getter private final Result result = new Result();

    public class Result implements MatchResult {
        public boolean isResult;
    }
    
    public AgingMatcher(AgingRecipe recipe, ItemStack ingredient) {
        
        if (recipe.getNbtIngredient().test(ingredient) == false) return;
        
        result.isResult = true;
    }

}
