package com.zener.brewery.recipes.distilling;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.zener.brewery.Brewery;
import com.zener.brewery.recipes.RecipeUtil;
import com.zener.brewery.recipes.cauldron.FailedOutput;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredient;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredientManager;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredientType;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;

public class DistillingRecipeSerializer implements RecipeSerializer<DistillingRecipe> {

    private static DefaultedList<NbtIngredient> readIngredients(JsonArray ingredients) {
        DefaultedList<NbtIngredient> ingredientList = DefaultedList.of();

        ingredients.forEach(json -> {
            NbtIngredient ingredient = DistillingIngredient.fromJson(json);
            if (ingredient.getMatchingStacks() != null && ingredient.getMatchingStacks().length > 0) {
                ingredientList.add(ingredient);
                Brewery.getNbtIngredientManager();
                NbtIngredientManager.apply(ingredient.getId(), ingredient);
            }
        });

        return ingredientList;
    }

    private static DefaultedList<FailedOutput> readFailedOutputs(Identifier id, JsonArray failedOutputs) {
        DefaultedList<FailedOutput> failedOutputList = DefaultedList.of();

        failedOutputs.forEach(json -> {
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                final ItemStack output = new ItemStack(JsonHelper.getItem(obj, "item"), JsonHelper.getInt(obj, "count", 1));
                if (obj.has("data")) {
                    NbtCompound nbt = RecipeUtil.json2nbt(JsonHelper.getObject(obj, "data"));
                    output.setNbt(nbt);
                }
                final double percentage = JsonHelper.getDouble(obj, "percentage", 90.0D);
                failedOutputList.add(new FailedOutput(output, percentage));
            } else {
                throw new JsonParseException("Failed Output must be a Json Object." + id.getPath());
            }
        });

        return failedOutputList;
    }

    @Override
    public DistillingRecipe read(Identifier id, JsonObject json) {
        final String groupIn = JsonHelper.getString(json, "group", "");

        JsonArray ingredients = JsonHelper.getArray(json, "ingredients");
        if (ingredients.size() == 0) throw new JsonParseException("No recipes found Distilling Recipe: " + id.getPath());

        final DefaultedList<NbtIngredient> inputItemsIn = readIngredients(ingredients);
        if (inputItemsIn.isEmpty()) {
            throw new JsonParseException("No NbtIngredients in Distilling Recipe: " + id.getPath());
        } else if (RecipeUtil.getNbtIngredientsCount(inputItemsIn) > DistillingRecipe.INPUT_SLOTS) {
            throw new JsonParseException("Too many NbtIngredients for Distilling Recipe:" + id.getPath() +" The max is " + DistillingRecipe.INPUT_SLOTS);
        }

        JsonObject jsonResult = JsonHelper.getObject(json, "result");

        final ItemStack outputIn = new ItemStack(JsonHelper.getItem(jsonResult, "item"), JsonHelper.getInt(jsonResult, "count", 1));
        if (jsonResult.has("data")) {
            NbtCompound nbt = RecipeUtil.json2nbt(JsonHelper.getObject(jsonResult, "data"));
            outputIn.setNbt(nbt);
        }

        JsonArray failures = JsonHelper.getArray(json, "failed_results", new JsonArray());

        DefaultedList<FailedOutput> failedOutputsIn = readFailedOutputs(id, failures);
        if (inputItemsIn.isEmpty()) {
            failedOutputsIn = null;
        }

        final byte distills = JsonHelper.getByte(json, "distills", (byte) 1);

        Brewery.LOGGER.info("Loading Brewing Recipe: " + id.getPath());

        return new DistillingRecipe(id, groupIn, inputItemsIn.get(0), distills, outputIn, failedOutputsIn);
    }

    @Override
    public DistillingRecipe read(final Identifier id, PacketByteBuf buf) {
        final String groupIn = buf.readString(32767);

        int ingredientSize = buf.readInt();
        final DefaultedList<NbtIngredient> ingredients = DefaultedList.of();
        for (int j = 0; j < ingredientSize; j++) {
            ingredients.add(NbtIngredient.fromPacket(id, buf, NbtIngredientType.BREWING));
        }
        final ItemStack outputIn = buf.readItemStack();

        int failedOutputSize = buf.readInt();
        DefaultedList<FailedOutput> failedOutputs = null;
        if (failedOutputSize != 0){
            failedOutputs = DefaultedList.of();
            for (int j = 0; j < failedOutputSize; j++) {
                failedOutputs.add(FailedOutput.read(buf));
            }
        }

        byte distills = buf.readByte();

        return new DistillingRecipe(id, groupIn, ingredients.get(0), distills, outputIn, failedOutputs);
    }

    @Override
    public void write(PacketByteBuf buf, DistillingRecipe recipe) {
        buf.writeString(recipe.getGroup());

        buf.writeVarInt(1);
        recipe.getNbtIngredient().write(buf);

        buf.writeItemStack(recipe.getOutput());

        if (recipe.getFailedOutputs() != null) {
            buf.writeVarInt(recipe.getFailedOutputs().size());
            for (FailedOutput failure : recipe.getFailedOutputs()) {
                failure.write(buf);
            }
        } else {
            buf.writeVarInt(0);
        }

        buf.writeByte(recipe.getDistills());
        
    }
    
}
