package com.zener.brewery;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import com.google.common.collect.ImmutableMap;
import com.zener.brewery.mixin.RecipeManagerAccessor;

public class BreweryClient implements ClientModInitializer {
    
    @Override
	public void onInitializeClient() {
		ClientLoginNetworking.registerGlobalReceiver(Brewery.PRESENCE_CHANNEL, (client, handler, buf, listenerAdder) -> {
			return CompletableFuture.completedFuture(new PacketByteBuf(Unpooled.buffer()));
		});

		ClientPlayNetworking.registerGlobalReceiver(Brewery.UPDATE_ADVANCED_RECIPES_PACKET_ID, (client, handler, buf, responseSender) -> {
			RecipeManager recipeManager = handler.getRecipeManager();
			Map<RecipeType<?>, Map<Identifier, Recipe<?>>> recipeMap = ((RecipeManagerAccessor) recipeManager).getRecipes();
			recipeMap = new HashMap<>(recipeMap);

			int recipeCount = buf.readVarInt();
			for (int i = 0; i < recipeCount; i++) {
				RecipeSerializer<?> serializer = Registry.RECIPE_SERIALIZER.get(buf.readIdentifier());
				Identifier id = buf.readIdentifier();

				Recipe<?> recipe = serializer.read(id, buf);
				Map<Identifier, Recipe<?>> recipeType = recipeMap.computeIfAbsent(recipe.getType(), rt -> new HashMap<>());
				recipeType.put(id, recipe);
			}
			Brewery.advancedIngredientSerializationEnabled.set(false);

			((RecipeManagerAccessor) recipeManager).setRecipes(ImmutableMap.copyOf(recipeMap));
		});
	}

	public static RecipeManager getClientRecipeManager() {
		return MinecraftClient.getInstance().getNetworkHandler().getRecipeManager();
	}
}
