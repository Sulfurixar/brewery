package com.zener.brewery.recipes.brewing;

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

public class BrewingRecipeSerializer implements RecipeSerializer<BrewingRecipe> {

    private static DefaultedList<NbtIngredient> readIngredients(JsonArray ingredients) {
        DefaultedList<NbtIngredient> ingredientList = DefaultedList.of();

        ingredients.forEach(json -> {
            NbtIngredient ingredient = BrewingIngredient.fromJson(json);
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
    public BrewingRecipe read(Identifier id, JsonObject json) {
        final String groupIn = JsonHelper.getString(json, "group", "");

        JsonArray ingredients = JsonHelper.getArray(json, "ingredients");
        if (ingredients.size() == 0) throw new JsonParseException("No recipes found Brewing Recipe: " + id.getPath());

        final DefaultedList<NbtIngredient> inputItemsIn = readIngredients(ingredients);
        if (inputItemsIn.isEmpty()) {
            throw new JsonParseException("No NbtIngredients in Brewing Recipe: " + id.getPath());
        } else if (RecipeUtil.getNbtIngredientsCount(inputItemsIn) > BrewingRecipe.INPUT_SLOTS) {
            throw new JsonParseException("Too many NbtIngredients for Brewing Recipe:" + id.getPath() +" The max is " + BrewingRecipe.INPUT_SLOTS);
        }

        JsonObject jsonResult = JsonHelper.getObject(json, "result");

        final ItemStack outputIn = new ItemStack(JsonHelper.getItem(jsonResult, "item"), JsonHelper.getInt(jsonResult, "count", 1));
        if (jsonResult.has("data")) {
            NbtCompound nbt = RecipeUtil.json2nbt(JsonHelper.getObject(jsonResult, "data"));
            outputIn.setNbt(nbt);
        }

        JsonObject catalystObject = JsonHelper.getObject(json, "catalyst");
        final NbtIngredient catalyst = BrewingIngredient.fromJson(catalystObject);

        JsonArray failures = JsonHelper.getArray(json, "failed_results", new JsonArray());

        DefaultedList<FailedOutput> failedOutputsIn = readFailedOutputs(id, failures);
        if (inputItemsIn.isEmpty()) {
            failedOutputsIn = null;
        }

        Brewery.LOGGER.info("Loading Brewing Recipe: " + id.getPath());

        return new BrewingRecipe(id, groupIn, inputItemsIn.get(0), catalyst, outputIn, failedOutputsIn);
    }

    @Override
    public BrewingRecipe read(final Identifier id, PacketByteBuf buf) {
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

        final NbtIngredient catalyst = NbtIngredient.fromPacket(id, buf, NbtIngredientType.BREWING);

        return new BrewingRecipe(id, groupIn, ingredients.get(0), catalyst, outputIn, failedOutputs);
    }

    @Override
    public void write(PacketByteBuf buf, BrewingRecipe recipe) {
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

        recipe.getCatalyst().write(buf);
        
    }
    
}
