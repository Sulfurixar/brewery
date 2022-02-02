package com.zener.brewery.networking;

import com.zener.brewery.Brewery;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.PacketByteBuf;

public class RecipeSync {

    private static final IntSet hasModClientConnectionHashes = IntSets.synchronize(new IntAVLTreeSet());
    
    public RecipeSync() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
			sender.sendPacket(Brewery.PRESENCE_CHANNEL, new PacketByteBuf(Unpooled.buffer()));
		});
		ServerLoginConnectionEvents.DISCONNECT.register((handler, server) -> {
			hasModClientConnectionHashes.remove(handler.getConnection().hashCode());
		});
		ServerLoginNetworking.registerGlobalReceiver(Brewery.PRESENCE_CHANNEL, (server, handler, understood, buf, synchronizer, responseSender) -> {
			if (understood) {
				hasModClientConnectionHashes.add(handler.getConnection().hashCode());
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (hasModClientConnectionHashes.contains(handler.getConnection().hashCode())) {
				((IServerPlayerEntity) handler.player).Brewery$setClientModPresent(true);
				hasModClientConnectionHashes.remove(handler.getConnection().hashCode());
			}
		});
    }
}
