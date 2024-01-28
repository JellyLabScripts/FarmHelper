package com.jelly.farmhelperv2.mixin.pathfinder;

import com.jelly.farmhelperv2.event.ChunkServerLoadEvent;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkProviderClient.class)
public class MixinChunkProviderClient {
    @Inject(method = "loadChunk(II)Lnet/minecraft/world/chunk/Chunk;", at = @At("RETURN"))
    private void onLoadChunk(int x, int z, CallbackInfoReturnable<Chunk> cir) {
        Chunk chunk = cir.getReturnValue();
        if (!chunk.isLoaded()) return;
        ChunkServerLoadEvent event = new ChunkServerLoadEvent(x, z, chunk);
        MinecraftForge.EVENT_BUS.post(event);
    }
}
