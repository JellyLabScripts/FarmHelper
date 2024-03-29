package com.jelly.farmhelperv2.util.helper;

import com.jelly.farmhelperv2.mixin.gui.AccessorGuiEditSign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.util.ChatComponentText;

public class SignUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void setTextToWriteOnString(String text) {
        if (!(mc.currentScreen instanceof GuiEditSign)) return;
        AccessorGuiEditSign guiEditSign = (AccessorGuiEditSign) mc.currentScreen;
        if (guiEditSign.getTileSign() == null || guiEditSign.getTileSign().signText[0].getUnformattedText().equals(text))
            return;

        guiEditSign.getTileSign().signText[0] = new ChatComponentText(text);
    }

    public static void confirmSign() {
        if (!(mc.currentScreen instanceof GuiEditSign)) return;
        AccessorGuiEditSign guiEditSign = (AccessorGuiEditSign) mc.currentScreen;
        guiEditSign.getTileSign().markDirty();
        mc.displayGuiScreen(null);
    }
}
