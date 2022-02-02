package com.zener.brewery.recipes.cauldron;

import java.util.Optional;

import com.zener.brewery.Brewery;
import com.zener.brewery.entity.CauldronBlockEntity;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@SuppressWarnings("unused")
public class Use {

    private final BlockState state;
    private final World world;
    private final BlockPos pos;
    private final PlayerEntity player;
    private final Hand hand;
    private final BlockHitResult hit;
    private final CallbackInfoReturnable<ActionResult> ci;
    private final int level;

    private final boolean validState;
    
    public Use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> ci) {
        this.state = state; this.world = world; this.pos = pos; this.player = player; this.hand = hand; this.hit = hit; this.ci = ci;
        level = state.get(LeveledCauldronBlock.LEVEL);
        validState = check();
    }

    public boolean cancelCallback() {
        return validState;
    } 

    private boolean check() {
        if (level <= 0) return false;
        Optional<CauldronBlockEntity> optional = world.getBlockEntity(pos, Brewery.CAULDRON_BLOCK_ENTITY);
        if (!optional.isPresent()) return false;

        CauldronBlockEntity cauldron = optional.get();
        return cauldron.playerInteract(player, hand, level);
    }

    private void log(String str) {
        Brewery.LOGGER.info("Cauldron Use: " + str);
    }

}
