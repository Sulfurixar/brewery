package com.zener.brewery.recipes.nbt_ingredient;

import java.util.stream.Stream;

import com.zener.brewery.Brewery;

import net.minecraft.util.Identifier;

public class EmptyIngredient extends NbtIngredient {
    
    public EmptyIngredient(Stream<? extends Entry> entries) {
        super(new Identifier(Brewery.MOD_ID+"_empty"), entries, NbtIngredientType.EMPTY);
    }

}