package com.zener.brewery.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

@Mixin(Registry.class)
public interface RegistryAccessor {
    
    @Invoker("createRegistryKey")
    public static <T> RegistryKey<Registry<T>> invokeCreateRegistryKey(String registryId) {
        throw new AssertionError();
    }

    @Invoker("create")
    public static <T> Registry<T> invokeCreate(RegistryKey<? extends Registry<T>> key, Supplier<T> defaultEntry) {
        throw new AssertionError();
    }
}
