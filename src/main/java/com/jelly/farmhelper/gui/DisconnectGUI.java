package com.jelly.farmhelper.gui;

import com.jelly.farmhelper.features.BanwaveChecker;
import com.jelly.farmhelper.gui.components.GuiBetterButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class DisconnectGUI extends GuiScreen {
    int buttonWidth = 90;
    int buttonHeight = 30;
    String message = "In banwave, disconnected from Hypixel";
    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.add(new GuiBetterButton(0, this.width/2 - buttonWidth/2, this.height/2 - buttonHeight/2, buttonWidth, buttonHeight, "Disable script"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        mc.fontRendererObj.drawString(BanwaveChecker.getBanDisplay(), this.width - mc.fontRendererObj.getStringWidth(BanwaveChecker.getBanDisplay())/2 - 5, this.height - 10, -1);
        mc.fontRendererObj.drawString(message, this.width - mc.fontRendererObj.getStringWidth(message)/2,20,  -1);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(button.id == 0){
            mc.displayGuiScreen(new GuiMainMenu());
        }
    }
}
