package com.zener.brewery.matcher;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.zener.brewery.Brewery;
import com.zener.brewery.entity.CauldronBlockEntity;
import com.zener.brewery.entity.CauldronBlockEntity.invStackProps;
import com.zener.brewery.recipes.RecipeUtil;
import com.zener.brewery.recipes.cauldron.CauldronRecipe;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredient;

import lombok.Getter;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class CauldronMatcher {

    private final Gson gson = new Gson();
    private final Type type = new TypeToken<Map<String, Object>>(){}.getType();
    
    private final CauldronBlockEntity cauldron;
    private final CauldronRecipe recipe;

    @Getter private final Result result = new Result();
    
    private final Map<ItemStack, Map<String, Object>> invStacks;
    private final Map<ItemStack, Map<String, Object>> ingStacks;

    public class Result implements MatchResult {
        public double result = 0.0;
        @Getter private List<ItemStack> matching;
        @Getter private int ingredientCount;

        public final double calculate() {
            return Math.abs(result - ingredientCount);
        }
    }

    public CauldronMatcher(CauldronBlockEntity cauldron, CauldronRecipe recipe) {
        this.cauldron = cauldron; this.recipe = recipe;
        invStacks = processItemStacks();
        ingStacks = processIngredients();
        calculateResult();
    }

    private final Map<ItemStack, Map<String, Object>> processItemStacks() {
        return cauldron.getInventory().stream().map(
            (Function<? super ItemStack, ? extends Entry>) stack -> {
                Map<String, Object> obj = gson.fromJson(stack.getOrCreateNbt().asString(), type);
                return new Entry(stack, RecipeUtil.FlatMapUtil.flatten(obj));
            }).collect(HashMap::new, (m, e) -> m.put(e.stack, e.nbt), HashMap::putAll);
    }

    private final Map<ItemStack, Map<String, Object>> processIngredients() {
        return recipe.getNbtIngredients().stream().map(
            (Function<? super NbtIngredient, ? extends Entry>) ingredient -> {
                ItemStack stack = ingredient.getMatchingStacks()[0];
                Map<String, Object> obj = gson.fromJson(stack.getOrCreateNbt().asString(), type);
                return new Entry(stack, RecipeUtil.FlatMapUtil.flatten(obj));
            }).collect(HashMap::new, (m, e) -> m.put(e.stack, e.nbt), HashMap::putAll);
    }

    private final class Entry {
        public final ItemStack stack;
        public final Map<String, Object> nbt;
        public Entry(ItemStack stack, Map<String, Object> nbt) {
            this.stack = stack; this.nbt = nbt;
        }
    }

    private final double getIngredientMatch(List<ItemStack> matching, List<ItemStack> not_matching) {

        double mismatch = 0;

        for (int i = 0; i < ingStacks.size(); i++) {
            ItemStack ingredient = ingStacks.keySet().toArray(new ItemStack[ingStacks.size()])[i];
            double count = ingredient.getCount();
            this.result.ingredientCount += count;

            for (int j = 0; j < not_matching.size(); j++) {
                if (count == 0) break;
                ItemStack stack = not_matching.get(j);
                MapDifference<String, Object> diff = Maps.difference(ingStacks.get(ingredient), invStacks.get(stack));
                if (diff.entriesDiffering().size() > 0) continue;
                if (diff.entriesOnlyOnLeft().size() > 0) continue;

                not_matching.remove(stack);
                matching.add(stack);

                invStackProps props = new invStackProps(stack);

                double estimateAmount = 1;

                // include ingredient confidence in estimate amount
                if (props.hasConfidence()) {
                    estimateAmount = props.calculateRandomPurity();
                }
                //

                if (props.hasUp()) {
                    double up = props.getUp();
                    double pureUp = up*estimateAmount;
                    if (pureUp >= count) {
                        up -= count;
                        count = 0;
                    } else {
                        count -= pureUp;
                    }
                } else {
                    if (count < estimateAmount) {
                        count = 0;
                    } else {
                        count -= estimateAmount;
                    }
                }
            }

            mismatch += count;
        }

        return mismatch;

    }

    private final double getIngredientMismatch(List<ItemStack> not_matching) {

        double mismatch = 0;

        for (int i = 0; i < not_matching.size(); i++) {
            ItemStack stack = not_matching.get(i);
            
            NbtCompound nbt = stack.getOrCreateNbt();
            if (nbt.contains(Brewery.MOD_ID) && nbt.get(Brewery.MOD_ID).getType() == NbtType.DOUBLE) {
                mismatch += nbt.getDouble(Brewery.MOD_ID);
            } else {
                mismatch += 1;
            }
        }

        return mismatch;

    }

    private final void calculateResult() {

        List<ItemStack> matching = new ArrayList<ItemStack>();
        List<ItemStack> not_matching = invStacks.keySet().stream().collect(ArrayList::new, (a, x) -> a.add(x), ArrayList::addAll);

        this.result.result = getIngredientMatch(matching, not_matching) + getIngredientMismatch(not_matching);
        this.result.matching = matching;
    }

}
