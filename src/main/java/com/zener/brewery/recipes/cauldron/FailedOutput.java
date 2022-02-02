package com.zener.brewery.recipes.cauldron;

import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;

public class FailedOutput {

    @Getter private final ItemStack output;
    @Getter private final double completionPercentage;

    public FailedOutput(ItemStack output, double percentage) {
        // FIX percentage to 0-100
        percentage = Math.max(percentage, 0.0D);
        percentage = Math.min(percentage, 100.0D);

        this.output = output; completionPercentage = percentage;
    }

    public void write(PacketByteBuf buf) {
        buf.writeItemStack(output);
        buf.writeDouble(completionPercentage);
    }

    public static FailedOutput read(PacketByteBuf buf) {
        ItemStack output = buf.readItemStack();
        double percentage = buf.readDouble();
        return new FailedOutput(output, percentage);
    }
    
}
