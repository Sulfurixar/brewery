package com.zener.brewery.recipes.brewing_stand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zener.brewery.Brewery;
import com.zener.brewery.items.Lore;
import com.zener.brewery.recipes.brewing.BrewingRecipe;
import com.zener.brewery.recipes.distilling.DistillingRecipe;
import com.zener.brewery.matcher.BrewingMatcher;
import com.zener.brewery.matcher.DistillingMatcher;
import com.zener.brewery.matcher.MatchResult;
import com.zener.brewery.mixin.RecipeManagerAccessor;
import com.zener.brewery.recipes.RecipeTypesRegistry;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredient;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredientManager;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredientType;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blue.endless.jankson.annotation.Nullable;
import lombok.Getter;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

public class Use {

    static Text distillTextPrep = Text.of("Distills:");

    public static boolean canCraft(DefaultedList<ItemStack> slots, World world) {
        ItemStack itemStack = slots.get(3);
        Brewery.getNbtIngredientManager();

        // distilling or brewing
        if (itemStack.isEmpty()) {
            // distilling
            for (int i = 0; i < 3; ++i) {
                ItemStack itemStack2 = slots.get(i);
                List<NbtIngredient> matches = NbtIngredientManager.getAllMatches(NbtIngredientType.DISTILLING, itemStack2);
                if (itemStack2.isEmpty() || matches.size() > 1 || matches.size() == 0 || matches.isEmpty()) continue;
                ItemStack[] stacks = matches.get(0).getMatchingStacks();
                if (stacks.length  == 0) continue;
                return true;
            }
        } else {
            for (int i = 0; i < 3; ++i) {
                ItemStack itemStack2 = slots.get(i);
                List<NbtIngredient> matches = NbtIngredientManager.getAllMatches(NbtIngredientType.BREWING, itemStack, itemStack2, world);
                if (itemStack2.isEmpty() || matches.size() > 1 || matches.size() == 0 || matches.isEmpty()) continue;
                ItemStack[] stacks = matches.get(0).getMatchingStacks();
                if (stacks.length  == 0) continue;
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Recipe<Inventory>, M extends MatchResult> Map<T, M> getRecipeMap(World world, RecipeType<Recipe<Inventory>> type, ItemStack ingredient, @Nullable ItemStack catalyst) {
        RecipeManager recipeManager = world.getRecipeManager();
        RecipeManagerAccessor accessor = (RecipeManagerAccessor) recipeManager;
        Map<T, M> results = new HashMap<T, M>();

        accessor.callGetAllOfType(type).forEach((id, recipe) -> {
            M match;
            if (type.equals(RecipeTypesRegistry.BREWING_RECIPE_SERIALIZER.type())) {
                match = (M)((BrewingRecipe) recipe).advancedMatches(ingredient, catalyst).getResult();
                results.put((T)recipe, match);
            }
            if (type.equals(RecipeTypesRegistry.DISTILLING_RECIPE_SERIALIZER.type())) {
                match = (M)((DistillingRecipe) recipe).advancedMatches(ingredient).getResult();
                results.put((T)recipe, match);
            }
        });
        return results;
    }

    private static class Distill {

        private final ItemStack stack;
        @Getter private final ItemStack output;

        public Distill(ItemStack stack, DistillingRecipe recipe) {
            this.stack = stack;

            byte distills = (byte) (getDistills() + 1);
            
            if (distills < recipe.getDistills()) {
                output = stack.copy();
                NbtCompound nbt = stack.getOrCreateNbt().copy();
                nbt.putByte("distills", distills);
                nbt = Lore.of(distillTextPrep.getString(), distillTextPrep.shallowCopy().setStyle(Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(0xFFAA00))).append(" "+Integer.toString(distills)), nbt);
                output.setNbt(nbt);
            } else {
                ItemStack o = recipe.getOutput();
                output = o.copy();
                output.setNbt(o.getOrCreateNbt().copy());
            }
        }

        private byte getDistills() {
            if (!checkDistills()) return 0;
            byte distills = stack.getNbt().getByte("distills");
            if (distills < 0) {
                distills = 0;
            }
            return distills;
        }

        private boolean checkDistills() {
            NbtCompound nbt = stack.getOrCreateNbt();
            return nbt.contains("distills") && nbt.get("distills").getType() == NbtType.BYTE;
        }

    }

    public static void craft(World world, BlockPos pos, DefaultedList<ItemStack> slots, CallbackInfo ci) {
        ItemStack catalyst = slots.get(3);

        if (catalyst.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                ItemStack ingredient = slots.get(i);
                if (ingredient.isEmpty()) continue;
                Map<DistillingRecipe, DistillingMatcher.Result> results = getRecipeMap(world, RecipeTypesRegistry.DISTILLING_RECIPE_SERIALIZER.type(), ingredient, null);
                if (results.size() > 0) {
                    DistillingMatcher.Result[] results_arr =  results.values().toArray(new DistillingMatcher.Result[results.size()]);
                    for (int j = 0; j < results_arr.length; j++) {
                        DistillingMatcher.Result result = results_arr[j];
                        if (result.isResult) {
                            slots.set(i, new Distill(ingredient, results.keySet().toArray(new DistillingRecipe[results.size()])[j]).output);
                            break;
                        }
                    }
                }
            }
            world.syncWorldEvent(WorldEvents.BREWING_STAND_BREWS, pos, 0);
            ci.cancel();
        } else {
            for (int i = 0; i < 3; i++) {
                ItemStack ingredient = slots.get(i);
                if (ingredient.isEmpty()) continue;
                Map<BrewingRecipe, BrewingMatcher.Result> results = getRecipeMap(world, RecipeTypesRegistry.BREWING_RECIPE_SERIALIZER.type(), ingredient, catalyst);
                if (results.size() > 0) {
                    BrewingMatcher.Result[] results_arr = results.values().toArray(new BrewingMatcher.Result[results.size()]);
                    for (int j = 0; j < results_arr.length; j++) {
                        BrewingMatcher.Result result = results_arr[j];
                        if (result.isResult) {
                            ItemStack o = results.keySet().toArray(new BrewingRecipe[results.size()])[j].getOutput();
                            ItemStack output = o.copy();
                            output.setNbt(o.getOrCreateNbt().copy());
                            slots.set(i, output);
                            break;
                        }
                    }
                }
            }
            catalyst.decrement(1);
            if (catalyst.getItem().hasRecipeRemainder()) {
                ItemStack i = new ItemStack(catalyst.getItem().getRecipeRemainder());
                if (catalyst.isEmpty()) {
                    catalyst = i;
                } else {
                    ItemScatterer.spawn(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), i);
                }
            }
            slots.set(3, catalyst);
            world.syncWorldEvent(WorldEvents.BREWING_STAND_BREWS, pos, 0);
            ci.cancel();
        }
    }
    
}
