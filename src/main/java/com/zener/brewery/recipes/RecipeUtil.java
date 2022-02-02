package com.zener.brewery.recipes;

import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredient;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.util.collection.DefaultedList;

public final class RecipeUtil {
    public final class FlatMapUtil {
        public static Map<String, Object> flatten(Map<String, Object> map) {
            return map.entrySet().stream()
                    .flatMap(FlatMapUtil::flatten)
                    .collect(LinkedHashMap::new, (m, e) -> m.put("/" + e.getKey(), e.getValue()), LinkedHashMap::putAll);
        }

        private static Stream<Map.Entry<String, Object>> flatten(Map.Entry<String, Object> entry) {

            if (entry == null) {
                return Stream.empty();
            }
    
            if (entry.getValue() instanceof Map<?, ?>) {
                return ((Map<?, ?>) entry.getValue()).entrySet().stream()
                        .flatMap(e -> flatten(new AbstractMap.SimpleEntry<>(entry.getKey() + "/" + e.getKey(), e.getValue())));
            }
    
            if (entry.getValue() instanceof List<?>) {
                List<?> list = (List<?>) entry.getValue();
                return IntStream.range(0, list.size())
                        .mapToObj(i -> new AbstractMap.SimpleEntry<String, Object>(entry.getKey() + "/" + i, list.get(i)))
                        .flatMap(FlatMapUtil::flatten);
            }
    
            return Stream.of(entry);
        }
    }

    public static MapDifference<String, Object> getNbtDifference(NbtCompound nbt_left, NbtCompound nbt_right) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();

        Map<String, Object> left = gson.fromJson(nbt_left.asString(), type);
        Map<String, Object> right = gson.fromJson(nbt_right.asString(), type);

        left = FlatMapUtil.flatten(left);
        right = FlatMapUtil.flatten(right);

        return Maps.difference(left, right);
    }

    public static byte getNbtIngredientsCount(DefaultedList<? extends NbtIngredient> ingredients) {
        byte count = 0;
        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack[] arr = ingredients.get(i).getMatchingStacks();
            if (arr.length > 1) {
                count++;
                continue;
            }
            count += arr[0].getCount();            
        }
        return count;
    }

    public static NbtCompound json2nbt(JsonObject obj) {
        try {
            NbtCompound nbt = StringNbtReader.parse(obj.toString());
            return nbt;
        } catch (CommandSyntaxException e) {
            throw new JsonParseException("Invalid NBT: " + e.getMessage());
        }
    }
}
