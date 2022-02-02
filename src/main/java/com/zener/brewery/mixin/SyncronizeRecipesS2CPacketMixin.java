package com.zener.brewery.mixin;

import java.util.Collection;
import java.util.List;

import com.zener.brewery.recipes.aging.AgingRecipe;
import com.zener.brewery.recipes.brewing.BrewingRecipe;
import com.zener.brewery.recipes.cauldron.CauldronRecipe;
import com.zener.brewery.recipes.distilling.DistillingRecipe;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.packet.s2c.play.SynchronizeRecipesS2CPacket;
import net.minecraft.recipe.Recipe;

@Mixin(SynchronizeRecipesS2CPacket.class)
public abstract class SyncronizeRecipesS2CPacketMixin {
    @Final
	@Shadow
	private List<Recipe<?>> recipes;

	@Inject(method = "<init>(Ljava/util/Collection;)V", at = @At("RETURN"))
	public void onCreated(Collection<Recipe<?>> recipes, CallbackInfo ci) {
		this.recipes.removeIf(recipe -> recipe instanceof CauldronRecipe);
		this.recipes.removeIf(recipe -> recipe instanceof BrewingRecipe);
		this.recipes.removeIf(recipe -> recipe instanceof DistillingRecipe);
		this.recipes.removeIf(recipe -> recipe instanceof AgingRecipe);
	}
}
