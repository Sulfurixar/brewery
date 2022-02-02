package com.zener.brewery.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class MurkyPotion {

    private final static NbtCompound getNbt() {
        NbtCompound nbt = new NbtCompound();

		NbtList CustomPotionEffects = new NbtList();

		CustomPotionEffects.add(createEffect(9));
		CustomPotionEffects.add(createEffect(18));
		nbt.put("CustomPotionEffects", CustomPotionEffects);
		
		NbtCompound display = new NbtCompound();
		display.putString("Name", "[{\"text\":\"Murky Liquid\", \"italic\":false}]");
		NbtList lore = new NbtList();
		lore.add(NbtString.of("I wonder how it would taste like..."));
		display.put("Lore", lore);
		nbt.put("display", display);
		nbt.putInt("CustomPotionColor", 3492663);
		nbt.putInt("CustomModelData", 1);
		nbt.putInt("HideFlags", 32);

        return nbt;
    }

    public static final ItemStack getItemStack() {
        Item m = Items.POTION;

        ItemStack s = new ItemStack(m, 1);
        s.setNbt(getNbt());

        return s;
    }

    private static final NbtCompound createEffect(int id) {
		NbtCompound effect = new NbtCompound();
		effect.putInt("Id", id);
		effect.putInt("Duration", 1200);
		effect.putByte("ShowParticles", (byte)0);

		return effect;
	}
    
}
