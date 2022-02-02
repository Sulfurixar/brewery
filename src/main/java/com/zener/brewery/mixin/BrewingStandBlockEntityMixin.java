package com.zener.brewery.mixin;

import com.zener.brewery.recipes.brewing_stand.Use;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityMixin {

    /*
    public static void tick(World world, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity) {
        ItemStack itemStack = blockEntity.inventory.get(4);
        if (blockEntity.fuel <= 0 && itemStack.isOf(Items.BLAZE_POWDER)) {
            blockEntity.fuel = 20;
            itemStack.decrement(1);
            BrewingStandBlockEntity.markDirty(world, pos, state);
        }
        boolean bl = BrewingStandBlockEntity.canCraft(blockEntity.inventory); // change to  BrewingStandBlockEntity.canCraft(blockEntity.inventory) || myFunc(blockEntity.inventory, world)
    */

    @ModifyVariable(method = "tick(ZLnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BrewingStandBlockEntity;)V", at = @At("STORE"), ordinal = 0)
    private static boolean modifyCraft(boolean b, World world, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity) {
        return b || breweryCraft(((IBrewingStandBlockEntity)blockEntity).getInventory(), world);
    }


    private static boolean breweryCraft(DefaultedList<ItemStack> slots, World world) {
        return Use.canCraft(slots, world);
    }

    
    @Inject(at = @At("HEAD"), cancellable = true, method = "craft(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/collection/DefaultedList;)V")
    private static void craft(World world, BlockPos pos, DefaultedList<ItemStack> slots, CallbackInfo ci) {

        Use.craft(world, pos, slots, ci);

        /*
        ItemStack itemStack = slots.get(3);
        for (int i = 0; i < 3; ++i) {
            // add in distilling recipes and custom brewing recipes
            // if air in slot 3, don't let normal recipe work
            slots.set(i, BrewingRecipeRegistry.craft(itemStack, slots.get(i)));
        }
        itemStack.decrement(1);
        if (itemStack.getItem().hasRecipeRemainder()) {
            ItemStack i = new ItemStack(itemStack.getItem().getRecipeRemainder());
            if (itemStack.isEmpty()) {
                itemStack = i;
            } else {
                ItemScatterer.spawn(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), i);
            }
        }
        slots.set(3, itemStack);
        world.syncWorldEvent(WorldEvents.BREWING_STAND_BREWS, pos, 0);
        */
    }
    
}
