package com.zener.brewery.recipes.aging;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.zener.brewery.Brewery;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredient;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredientType;

import blue.endless.jankson.annotation.Nullable;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;


public class AgingIngredient extends NbtIngredient {
    
    private final static NbtIngredientType<AgingIngredient> type = NbtIngredientType.AGING;

    public static final AgingIngredient EMPTY = new AgingIngredient(new Identifier(Brewery.MOD_ID+"_empty"), Stream.empty());

    public AgingIngredient(Identifier id, Stream<? extends Entry> entries) {
        super(id, entries, type);
    }

    public NbtIngredientType<?> getType() {
        return super.getType();
    }

    public static AgingIngredient fromJson(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull()) {
            throw new JsonSyntaxException("Item cannot be null");
        }
        if (json.isJsonObject()) {
            StackEntry entry = (StackEntry) AgingIngredient.entryFromJson(json.getAsJsonObject());
            
            return AgingIngredient.ofEntries(entry.getId(), Stream.of(entry));
        }
        if (json.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            if (jsonArray.size() == 0) {
                throw new JsonSyntaxException("Item array cannot be empty, at least one item must be defined");
            }
            TagEntry entry = (TagEntry) AgingIngredient.entryFromJson(json.getAsJsonObject());
            return AgingIngredient.ofEntries(entry.getId(), StreamSupport.stream(jsonArray.spliterator(), false).map(jsonElement -> entryFromJson(JsonHelper.asObject(jsonElement, "item"))));
        }
        throw new JsonSyntaxException("Expected item to be object or array of objects");
    }

    private static AgingIngredient ofEntries(Identifier id, Stream<? extends Entry> entries) {
        AgingIngredient ingredient = new AgingIngredient(id, entries);
        return ingredient.entries.length == 0 ? EMPTY : ingredient;
    }
    
}
