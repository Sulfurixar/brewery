package com.zener.brewery.mixin;

import com.zener.brewery.recipes.aging.Use;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.collection.DefaultedList;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    @Final
    @Shadow
    private ScreenHandlerType<?> type;

    @Final
    @Shadow
    private DefaultedList<ItemStack> trackedStacks;
    
    @Inject(method = "updateTrackedSlot(ILnet/minecraft/item/ItemStack;Ljava/util/function/Supplier;)V", at = @At("HEAD"))
    public void updateTrackedSlot(int slot, ItemStack stack, java.util.function.Supplier<ItemStack> copySupplier, CallbackInfo ci) {
        Use.transferSlotModif(stack, slot, type,  copySupplier, trackedStacks, (Object) this);
    }

}
