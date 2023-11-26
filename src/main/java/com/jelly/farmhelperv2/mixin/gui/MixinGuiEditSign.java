package com.jelly.farmhelperv2.mixin.gui;

import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.SignUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C12PacketUpdateSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


// Credits GTC
@Mixin(GuiEditSign.class)
public abstract class MixinGuiEditSign extends GuiScreen {

    @Shadow
    private TileEntitySign tileSign;

    public MixinGuiEditSign(TileEntitySign tileEntitySign) {
        tileSign = tileEntitySign;
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGui(CallbackInfo ci) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (SignUtils.getInstance().getTextToWriteOnString().isEmpty()) return;

        LogUtils.sendDebug("Sign text: " + SignUtils.getInstance().getTextToWriteOnString());
        tileSign.signText[0] = new ChatComponentText(SignUtils.getInstance().getTextToWriteOnString());

        NetHandlerPlayClient netHandlerPlayClient = mc.getNetHandler();
        if (netHandlerPlayClient != null) {
            netHandlerPlayClient.addToSendQueue(new C12PacketUpdateSign(tileSign.getPos(), tileSign.signText));
            SignUtils.getInstance().setTextToWriteOnString("");
            LogUtils.sendDebug("Sign text set to empty.");
        }
    }
}