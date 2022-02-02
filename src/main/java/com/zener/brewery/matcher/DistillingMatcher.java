package com.zener.brewery.matcher;

import com.zener.brewery.recipes.distilling.DistillingRecipe;

import lombok.Getter;
import net.minecraft.item.ItemStack;

public class DistillingMatcher {

    @Getter private final Result result = new Result();

    public class Result implements MatchResult {
        public boolean isResult;
    }
    
    public DistillingMatcher(DistillingRecipe recipe, ItemStack ingredient) {
        
        if (recipe.getNbtIngredient().test(ingredient) == false) return;
        
        result.isResult = true;
    }

}
