package com.zener.brewery.recipes.nbt_ingredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.zener.brewery.Brewery;
import com.zener.brewery.recipes.RecipeUtil;

import blue.endless.jankson.annotation.Nullable;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class NbtIngredient implements Predicate<ItemStack> {

    public static final NbtIngredient EMPTY = new NbtIngredient(new Identifier(Brewery.MOD_ID+"_empty"), Stream.empty(), NbtIngredientType.EMPTY);
    public final Entry[] entries;
    private ItemStack[] matchingStacks;
    @Nullable
    private IntList ids;
    @Getter private final NbtIngredientType<?> type;
    @Getter private final Identifier id;

    public boolean matches(PlayerEntity player, World world) {
        if (test(player.getMainHandStack())) {
            return true;
        }
        return false;
    }

    public NbtIngredient(Identifier id, Stream<? extends Entry> entries, NbtIngredientType<?> type) {
        this.entries = (Entry[])entries.toArray(Entry[]::new);
        this.type = type;
        this.id = id;
    }

    private void cacheMatchingStacks() {
        if (this.matchingStacks == null) {
            this.matchingStacks = (ItemStack[])Arrays.stream(this.entries).flatMap(entry -> entry.getStacks().stream()).distinct().toArray(ItemStack[]::new);
        }
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (stack == null) return false;
        this.cacheMatchingStacks();
        if (this.matchingStacks.length == 0) return stack.isEmpty();
        for (ItemStack ingredientStack : getMatchingStacks()) {
            if (!ingredientStack.isOf(stack.getItem())) continue;

            if (!ingredientStack.hasNbt()) return true;

            if (!stack.hasNbt()) continue;

            NbtCompound iNbt = ingredientStack.getNbt();
            NbtCompound sNbt = stack.getNbt();

            MapDifference<String, Object> diff = RecipeUtil.getNbtDifference(iNbt, sNbt);

            if (diff.entriesDiffering().size() > 0) continue;
            if (diff.entriesOnlyOnLeft().size() > 0) continue;

            return true;
        }

        return false;
    }

    public static Entry entryFromJson(JsonObject json) {
        if (json.has("item") && json.has("tag")) {
            throw new JsonParseException("An NbtIngredient entry is either a tag or an item, not both");
        }
        if (!json.has("id")) throw new JsonParseException("An NbtIngredient entry must have an ingredient id.");
        Identifier id = new Identifier(JsonHelper.getString(json, "id"));
        if (json.has("item")) {
            ItemStack stack = new ItemStack(ShapedRecipe.getItem(json));
            if (json.has("count")) {
                byte count = JsonHelper.getByte(json, "count");
                stack.setCount(count);
            }
            if (json.has("data")) {
                NbtCompound nbt = RecipeUtil.json2nbt(JsonHelper.getObject(json, "data"));
                stack.setNbt(nbt);
            }
            return new StackEntry(stack, id);
        }
        if (json.has("tag")) {
            Identifier item = new Identifier(JsonHelper.getString(json, "tag"));
            Tag<Item> tag = ServerTagManagerHolder.getTagManager().getTag(Registry.ITEM_KEY, item, identifier -> new JsonSyntaxException("Unknown item tag '" + identifier + "'"));
            return new TagEntry(tag, id);
        }
        throw new JsonParseException("An NbtIngredient entry needs either a tag or an item");
    }

    public static class StackEntry implements Entry {
        private final ItemStack stack;
        @Getter private final Identifier id;

        StackEntry(ItemStack stack, Identifier id) {
            this.stack = stack; this.id = id;
        }

        @Override
        public Collection<ItemStack> getStacks() {
            return Collections.singleton(this.stack);
        }

        @Override
        public JsonObject toJson() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("item", Registry.ITEM.getId(this.stack.getItem()).toString());
            return jsonObject;
        }
        
    }

    public static class TagEntry
    implements Entry {
        private final Tag<Item> tag;
        @Getter private final Identifier id;

        TagEntry(Tag<Item> tag, Identifier id) {
            this.tag = tag; this.id = id;
        }

        @Override
        public Collection<ItemStack> getStacks() {
            ArrayList<ItemStack> list = Lists.newArrayList();
            for (Item item : this.tag.values()) {
                list.add(new ItemStack(item));
            }
            return list;
        }

        @Override
        public JsonObject toJson() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("tag", ServerTagManagerHolder.getTagManager().getTagId(Registry.ITEM_KEY, this.tag, () -> new IllegalStateException("Unknown item tag")).toString());
            return jsonObject;
        }
    }

    public static interface Entry {
        public Collection<ItemStack> getStacks();
        public JsonObject toJson();

    }
    
    public JsonElement toJson() {
        if (this.entries.length == 1) {
            return this.entries[0].toJson();
        }
        JsonArray jsonArray = new JsonArray();
        for (Entry entry : this.entries) {
            jsonArray.add(entry.toJson());
        }
        return jsonArray;
    }

    public boolean isEmpty() {
        return !(this.entries.length != 0 || this.matchingStacks != null && this.matchingStacks.length != 0 || this.ids != null && !this.ids.isEmpty());
    }

    private static NbtIngredient ofEntries(Identifier id, Stream<? extends Entry> entries, NbtIngredientType<?> type) {
        NbtIngredient ingredient = new NbtIngredient(id, entries, type);
        return ingredient.entries.length == 0 ? EMPTY : ingredient;
    }

    public static NbtIngredient empty() {
        return EMPTY;
    }

    public static NbtIngredient ofItems(Identifier id, NbtIngredientType<?> type, ItemConvertible ... items) {
        return NbtIngredient.ofStacks(id, Arrays.stream(items).map(ItemStack::new), type);
    }

    public static NbtIngredient ofStacks(Identifier id, NbtIngredientType<?> type, ItemStack ... stacks) {
        return NbtIngredient.ofStacks(id, Arrays.stream(stacks), type);
    }

    public static NbtIngredient ofStacks(Identifier id, Stream<ItemStack> stacks, NbtIngredientType<?> type) {
        return NbtIngredient.ofEntries(id, stacks.filter(stack -> !stack.isEmpty()).map((stack) -> new StackEntry(stack, id)), type);
    }

    public static NbtIngredient fromTag(Identifier id, Tag<Item> tag, NbtIngredientType<?> type) {
        return NbtIngredient.ofEntries(id, Stream.of(new TagEntry(tag, id)), type);
    }

    public static NbtIngredient fromPacket(Identifier id, PacketByteBuf buf, NbtIngredientType<?> type) {
        return NbtIngredient.ofEntries(id, buf.readList(PacketByteBuf::readItemStack).stream().map((stack) -> new StackEntry(stack, id)), type);
    }

    public ItemStack[] getMatchingStacks() {
        this.cacheMatchingStacks();
        return this.matchingStacks;
    }

    public void write(PacketByteBuf buf) {
        this.cacheMatchingStacks();
        buf.writeCollection(Arrays.asList(this.matchingStacks), PacketByteBuf::writeItemStack);
    }

}
