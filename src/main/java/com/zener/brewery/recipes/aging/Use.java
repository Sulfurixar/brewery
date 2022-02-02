package com.zener.brewery.recipes.aging;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.zener.brewery.items.Lore;
import com.zener.brewery.matcher.AgingMatcher;
import com.zener.brewery.mixin.RecipeManagerAccessor;
import com.zener.brewery.recipes.RecipeTypesRegistry;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredient;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredientManager;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredientType;

import blue.endless.jankson.annotation.Nullable;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

public class Use {

    private static final SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
    static Text ageTextPrep = Text.of("Age:");

    public static boolean age(World world, DefaultedList<ItemStack> inv, int slot, ItemStack ingredient, BarrelBlockEntity barrel) {
        List<NbtIngredient> ingredients = NbtIngredientManager.getAllMatches(NbtIngredientType.AGING, ingredient);
        if (ingredients.isEmpty()) return false;
        RecipeManager recipeManager = world.getRecipeManager();
        RecipeManagerAccessor accessor = (RecipeManagerAccessor) recipeManager;
        Map<AgingRecipe, AgingMatcher.Result> results = new HashMap<AgingRecipe, AgingMatcher.Result>();

        accessor.callGetAllOfType(RecipeTypesRegistry.AGING_RECIPE_SERIALIZER.type()).forEach((id, recipe) -> {
            AgingMatcher.Result result = ((AgingRecipe) recipe).advancedMatches(ingredient).getResult();
            if (result.isResult) {
                results.put((AgingRecipe)recipe, result);
            }
        });

        if (results == null || results.isEmpty() || results.size() == 0) return false;

        Date cur_date = new Date();
        NbtCompound nbt = ingredient.getOrCreateNbt();

        if (nbt.contains("aa") && nbt.get("aa").getType() == NbtType.BYTE) {
            if (nbt.getByte("aa") == 0) {
                return false;
            }
        }
        System.out.println("Aging: " + Integer.toString(slot));
        if (nbt.contains("age") && nbt.get("age").getType() == NbtType.STRING) {
            try {
                Date saved_date = sdf1.parse(nbt.getString("age"));
                long diff = cur_date.getTime() - saved_date.getTime();
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                System.out.println("Minutes: " + Integer.toString((int) minutes));
                long mc_years = (minutes / 20) / 365;
                AgingRecipe[] recipes = results.keySet().toArray(new AgingRecipe[results.size()]);
                AgingRecipe recipe = recipes[0];
                if (minutes/20 >= recipe.getAge()) {
                    ItemStack o = recipe.getOutput();
                    ItemStack output = o.copy();
                    output.setNbt(o.getOrCreateNbt().copy());
                    inv.set(slot, output);
                    barrel.removeStack(slot);
                    barrel.setStack(slot, output);
                } else {
                    String timestring = " ";
                    if (mc_years >= 1) {
                        timestring += Integer.toString((int)mc_years) + " Years";
                    } else if (minutes/20 >= 1) {
                        timestring += Integer.toString((int)(minutes/20)) + " Days";
                    } else {
                        return true;
                    }
                    nbt = Lore.of(ageTextPrep.getString(), ageTextPrep.shallowCopy().setStyle(Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(0xFFAA00))).append(timestring), nbt);
                    ingredient.setNbt(nbt);
                }
                barrel.markDirty();
                return true;
            } catch (ParseException e) {
                return false;
            }
        } else {
            nbt.putString("age", sdf1.format(cur_date));
            barrel.markDirty();
        }

        return false;
    }

    public static void transferSlotModif(ItemStack item, int slot, @Nullable ScreenHandlerType<?> type, Supplier<ItemStack> copySupplier, DefaultedList<ItemStack> trackedStacks, Object handler) {
        if (type == null) {
            return;
        }
        if (type != ScreenHandlerType.GENERIC_9X3) return;

        // disallow aging if it was an aging item
        ItemStack itemStack = trackedStacks.get(slot);
        if (!ItemStack.areEqual(itemStack, item) && !item.isEmpty()) {
            System.out.println("Now: itemStack: " + itemStack.getName().asString() + " item: " + item.getName().asString() + " slot: " + Integer.toString(slot));
            if (slot > ((ScreenHandler) handler).slots.size() - 1 - 36) {
                NbtCompound nbt = item.getOrCreateNbt();
                if (nbt.contains("age") && (!nbt.contains("aa") || nbt.get("aa").getType() != NbtType.BYTE) || nbt.getByte("aa") != 0){
                    nbt.putBoolean("aa", false);
                    item.setNbt(nbt);
                }
            }
        }
    }
    
}
