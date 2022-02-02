package com.zener.brewery.items;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;

public class Lore {

    public static NbtCompound of(String filter, Text replacement_entry, NbtCompound nbt) {

        NbtCompound display;
        NbtList Lore;
        if (nbt.contains("display") && nbt.get("display").getType() == NbtType.COMPOUND) {
            display = nbt.getCompound("display");
            if (display.contains("Lore") && display.get("Lore").getType() == NbtType.LIST) {
                Lore = display.getList("Lore", NbtType.STRING);
                boolean not_found = true;
                for (int i = 0; i < Lore.size(); i++) {
                    Text text = Text.Serializer.fromJson(Lore.getString(i));
                    if (text.getString().startsWith(filter)) {
                        not_found = false;
                        Lore.remove(i);
                        Lore.add(i, NbtString.of(Text.Serializer.toJson(replacement_entry).toString()));
                    }
                }
                if (not_found) {
                    Lore.add(NbtString.of(Text.Serializer.toJson(replacement_entry).toString()));
                }
                display.put("Lore", Lore);
                nbt.put("display", display);
                return nbt;
            } else {
                Lore = new NbtList();
                Lore.add(NbtString.of(Text.Serializer.toJson(replacement_entry).toString()));
            }
            display.put("Lore", Lore);
        } else {
            display = new NbtCompound();
            Lore = new NbtList();
            Lore.add(NbtString.of(Text.Serializer.toJson(replacement_entry).toString()));
            display.put("Lore", Lore);
        }

        nbt.put("display", display);
        return nbt;

    }
    
}
