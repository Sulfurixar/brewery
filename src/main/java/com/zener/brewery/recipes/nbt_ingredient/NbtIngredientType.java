package com.zener.brewery.recipes.nbt_ingredient;

import java.util.Optional;
import com.zener.brewery.Brewery;
import com.zener.brewery.recipes.aging.AgingIngredient;
import com.zener.brewery.recipes.brewing.BrewingIngredient;
import com.zener.brewery.recipes.cauldron.CauldronIngredient;
import com.zener.brewery.recipes.distilling.DistillingIngredient;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public interface NbtIngredientType <T extends NbtIngredient> {
    public static final NbtIngredientType<EmptyIngredient> EMPTY = NbtIngredientType.register("empty");
    public static final NbtIngredientType<CauldronIngredient> CAULDRON = NbtIngredientType.register("cauldron");
    public static final NbtIngredientType<DistillingIngredient> DISTILLING = NbtIngredientType.register("distilling");
    public static final NbtIngredientType<BrewingIngredient> BREWING = NbtIngredientType.register("brewing");
    public static final NbtIngredientType<AgingIngredient> AGING = NbtIngredientType.register("aging");
    
    public static <T extends NbtIngredient> NbtIngredientType<T> register(final String id) {
        return Registry.register(Brewery.NBT_INGREDIENT_TYPE, new Identifier(id), new NbtIngredientType<T>(){

            public String toString() {
                return id;
            }
        });
    }

    public default NbtIngredientType<?> getType() {
        return NbtIngredientType.EMPTY;
    }

    // CAULDRON
    default public <C extends PlayerEntity> Optional<NbtIngredient> match(NbtIngredient ingredient, World world, C player) {
        return ingredient.matches(player, world) ? Optional.of(ingredient) : Optional.empty();
    }

    // DISTILLING
    default public Optional<NbtIngredient> match(NbtIngredient ingredient, ItemStack stack) {
        return ingredient.test(stack) ? Optional.of(ingredient) : Optional.empty();
    }

    // BREWING
    default public Optional<NbtIngredient> match(BrewingIngredient ingredient, ItemStack stack, ItemStack stack2, World world) {
        return ingredient.matches(stack, stack2, world)  ? Optional.of(ingredient) : Optional.empty();
    }

}
