package com.zener.brewery.recipes;

import java.util.function.Supplier;

import com.zener.brewery.Brewery;
import com.zener.brewery.recipes.aging.AgingRecipe;
import com.zener.brewery.recipes.brewing.BrewingRecipe;
import com.zener.brewery.recipes.brewing.BrewingRecipeSerializer;
import com.zener.brewery.recipes.cauldron.CauldronRecipe;
import com.zener.brewery.recipes.cauldron.CauldronRecipeSerializer;
import com.zener.brewery.recipes.aging.AgingRecipeSerializer;
import com.zener.brewery.recipes.distilling.DistillingRecipe;
import com.zener.brewery.recipes.distilling.DistillingRecipeSerializer;

import net.minecraft.inventory.Inventory;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public enum RecipeTypesRegistry {
    CAULDRON_RECIPE_SERIALIZER("cauldron", CauldronRecipe.class, CauldronRecipeSerializer::new),
    BREWING_RECIPE_SERIALIZER("brewing", BrewingRecipe.class, BrewingRecipeSerializer::new), 
    DISTILLING_RECIPE_SERIALIZER("distilling", DistillingRecipe.class, DistillingRecipeSerializer::new),
    AGING_RECIPE_SERIALIZER("aging", AgingRecipe.class, AgingRecipeSerializer::new);

    private final String pathName;
    private final Class<? extends Recipe<? extends Inventory>> recipeClass;
    private final Supplier<RecipeSerializer<? extends Recipe<? extends Inventory>>> recipeSerializerSupplier;
    private RecipeSerializer<? extends Recipe<? extends Inventory>> serializer;
    private RecipeType<? extends Recipe<? extends Inventory>> type;

    RecipeTypesRegistry(String pathName, Class<? extends Recipe<? extends Inventory>> recipeClass,
            Supplier<RecipeSerializer<? extends Recipe<? extends Inventory>>> recipeSerializerSupplier) {
        this.pathName = pathName;
        this.recipeClass = recipeClass;
        this.recipeSerializerSupplier = recipeSerializerSupplier;
    }

    public static void registerAll() {
        for (RecipeTypesRegistry value : values()) {
            Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(Brewery.MOD_ID, value.pathName), value.serializer());
            value.type = RecipeType.register(value.pathName);
        }
    }

    public RecipeSerializer<? extends Recipe<? extends Inventory>> serializer() {
        if (serializer == null) {
            serializer = recipeSerializerSupplier.get();
        }

        return serializer;
    }

    @SuppressWarnings("unchecked")
    public <T extends Recipe<? extends Inventory>> RecipeType<T> type() {
        return (RecipeType<T>) type(recipeClass);
    }

    @SuppressWarnings({"unchecked"})
    private <T extends Recipe<? extends Inventory>> RecipeType<T> type(Class<T> clazz) {
        if (type == null) {
            type = RecipeType.register(new Identifier(Brewery.MOD_ID, pathName).toString());
        }
        return (RecipeType<T>) type;
    }
    
}
