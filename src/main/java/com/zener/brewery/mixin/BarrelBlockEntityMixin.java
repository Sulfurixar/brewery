package com.zener.brewery.mixin;

import com.zener.brewery.recipes.aging.Use;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

@Mixin(BarrelBlockEntity.class)
public class BarrelBlockEntityMixin {

    @Shadow
    private DefaultedList<ItemStack> inventory;
    
    @Inject(at = @At("HEAD"), method = "tick()V")
    public void tick(CallbackInfo ci) {
        if (inventory.isEmpty()) return;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack ingredient = inventory.get(i);
            if (ingredient.isEmpty()) continue;            
            Use.age(((BlockEntity)(Object) this).getWorld(), inventory, i, ingredient, ((BarrelBlockEntity)((Object) this)));
        }
    }

}
