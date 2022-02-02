package com.zener.brewery;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import com.zener.brewery.config.ModConfig;
import com.zener.brewery.entity.CauldronBlockEntity;
import com.zener.brewery.items.MurkyPotion;
import com.zener.brewery.mixin.RegistryAccessor;
import com.zener.brewery.recipes.RecipeTypesRegistry;
import com.zener.brewery.recipes.cauldron.FailedOutput;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredientManager;
import com.zener.brewery.recipes.nbt_ingredient.NbtIngredientType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

public class Brewery implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.

	public static final String MOD_ID = "brewery";

	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	public static BlockEntityType<CauldronBlockEntity> CAULDRON_BLOCK_ENTITY;

	public static ModConfig CONFIG;

	public static final Identifier PRESENCE_CHANNEL = new Identifier(MOD_ID, "present");

	public static final Identifier UPDATE_ADVANCED_RECIPES_PACKET_ID = new Identifier(MOD_ID, "update_advanced_recipes");

	public static ThreadLocal<Boolean> advancedIngredientSerializationEnabled = new ThreadLocal<>();

	public static final RegistryKey<Registry<NbtIngredientType<?>>> NBT_INGREDIENT_TYPE_KEY = RegistryAccessor.invokeCreateRegistryKey("nbt_ingredient_type");
	public static final Registry<NbtIngredientType<?>> NBT_INGREDIENT_TYPE = RegistryAccessor.invokeCreate(NBT_INGREDIENT_TYPE_KEY, () -> NbtIngredientType.CAULDRON);
	@Getter private static final NbtIngredientManager nbtIngredientManager = new NbtIngredientManager();

	public static final FailedOutput DEFAULT_FAILED_OUTPUT = new FailedOutput(MurkyPotion.getItemStack(), 0);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		LOGGER.info("Initializing.");

		AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

		RecipeTypesRegistry.registerAll();

		CAULDRON_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, "cauldron", FabricBlockEntityTypeBuilder.create(CauldronBlockEntity::new, Blocks.WATER_CAULDRON).build(null));

	}
}
