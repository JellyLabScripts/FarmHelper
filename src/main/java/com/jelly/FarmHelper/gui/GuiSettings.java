package com.jelly.FarmHelper.gui;

import com.jelly.FarmHelper.config.Config;
import com.jelly.FarmHelper.gui.buttons.GuiCustomSwitchButton;
import com.jelly.FarmHelper.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

import java.awt.*;
import java.io.IOException;

public class GuiSettings extends GuiScreen {
    String title = "Farm Helper Settings";
    int gap = 30;
    int fieldWidth = 200;
    int fieldHeight = 15;

    private GuiTextField urlTextBox;
    private GuiTextField statusTimeBox;
    private GuiTextField jacobThresholdBox;
    private GuiTextField jacobMushroomBox;

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) { }

    @Override
    public void initGui() {
        super.initGui();
        int firstY = 30 + ((new ScaledResolution(mc).getScaledHeight() - (gap * 7)) / 2);
        this.buttonList.add(new GuiCustomSwitchButton(0, this.width/2 + 120, firstY, this.width/2 - 150, firstY + 2, 40, 15, "Auto ReSync"));
        this.buttonList.add(new GuiCustomSwitchButton(1, this.width/2 + 120, firstY + gap, this.width/2 - 150, firstY + gap + 2, 40, 15, "Jacob Failsafe"));
        jacobThresholdBox = new GuiTextField(1, Minecraft.getMinecraft().fontRendererObj, this.width/2 + 160 - 60, firstY + gap * 2, 60, fieldHeight);
        jacobThresholdBox.setMaxStringLength(8);
        jacobThresholdBox.setText(String.valueOf(Config.jacobThreshold));
        jacobThresholdBox.setFocused(false);
        jacobMushroomBox = new GuiTextField(1, Minecraft.getMinecraft().fontRendererObj, this.width/2 + 160 - 60, firstY + gap * 3, 60, fieldHeight);
        jacobMushroomBox.setMaxStringLength(8);
        jacobMushroomBox.setText(String.valueOf(Config.jacobMushroom));
        jacobMushroomBox.setFocused(false);
        this.buttonList.add(new GuiCustomSwitchButton(2, this.width/2 + 120, firstY + gap * 4, this.width/2 - 150, firstY + gap * 4 + 2, 40, 15, "Webhook Log"));
        urlTextBox = new GuiTextField(1, Minecraft.getMinecraft().fontRendererObj, this.width/2 + 160 - fieldWidth, firstY + gap * 5, fieldWidth, fieldHeight);
        urlTextBox.setMaxStringLength(256);
        urlTextBox.setText(Config.webhookUrl);
        urlTextBox.setFocused(false);
        statusTimeBox = new GuiTextField(1, Minecraft.getMinecraft().fontRendererObj, this.width/2 + 160 - 40, firstY + gap * 6, 40, fieldHeight);
        statusTimeBox.setMaxStringLength(5);
        statusTimeBox.setText(String.valueOf(Config.statusTime));
        statusTimeBox.setFocused(false);
        this.buttonList.add(new GuiCustomSwitchButton(4, this.width/2 + 120, firstY + gap * 7, this.width/2 - 150, firstY + gap * 7 + 2, 40, 15, "Auto Sell"));
        this.buttonList.add(new GuiCustomSwitchButton(3, this.width/2 + 120, firstY + gap * 8, this.width/2 - 150, firstY + gap * 8 + 2, 40, 15, "Debug Mode"));
        initialSelect();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int firstY = 30 + ((new ScaledResolution(mc).getScaledHeight() - (gap * 7)) / 2);
        // drawRect(0, 0, this.width, this.height, new Color(41, 41, 41, 255).getRGB());
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 225).getRGB());
        Utils.drawString(title, this.width / 2 - mc.fontRendererObj.getStringWidth(title) / 2 * 2, firstY - 50, 2, -1); // multiply by the size smh works firstY + gap * 3,this.width/2 - 150
        mc.fontRendererObj.drawStringWithShadow("Jacob Score Threshold", this.width/2 - 150, firstY + gap * 2 + 2, -1);
        mc.fontRendererObj.drawStringWithShadow("Jacob Mushroom Score Threshold", this.width/2 - 150, firstY + gap * 3 + 2, -1);
        mc.fontRendererObj.drawStringWithShadow("Webhook URL", this.width/2 - 150, firstY + gap * 5 + 2, -1);
        mc.fontRendererObj.drawStringWithShadow("Status Cooldown (min)", this.width/2 - 150, firstY + gap * 6 + 2, -1);
        urlTextBox.drawTextBox();
        statusTimeBox.drawTextBox();
        jacobThresholdBox.drawTextBox();
        jacobMushroomBox.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame(){
        return false;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) button;
            temp.switchSelect();
            Config.resync = !Config.resync;
            updateScreen();
            Config.writeConfig();
        }
        else if (button.id == 1) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) button;
            temp.switchSelect();
            Config.jacobFailsafe = !Config.jacobFailsafe;
            updateScreen();
            Config.writeConfig();
        }
        else if (button.id == 2) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) button;
            temp.switchSelect();
            Config.webhookLog = !Config.webhookLog;
            updateScreen();
            Config.writeConfig();
        }
        else if (button.id == 3) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) button;
            temp.switchSelect();
            Config.debug = !Config.debug;
            updateScreen();
            Config.writeConfig();
        }
        else if (button.id == 4) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) button;
            temp.switchSelect();
            Config.autosell = !Config.autosell;
            updateScreen();
            Config.writeConfig();
        }
    }

    void initialSelect() {
        if (Config.resync) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(0);
            temp.switchSelect();
        }
        if (Config.jacobFailsafe) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(1);
            temp.switchSelect();
        }
        if (Config.webhookLog) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(2);
            temp.switchSelect();
        }
        if (Config.debug) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(3);
            temp.switchSelect();
        }
        if (Config.autosell) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(4);
            temp.switchSelect();
        }
    }

    protected void keyTyped(char par1, int par2) {
        try {
            super.keyTyped(par1, par2);
            urlTextBox.textboxKeyTyped(par1, par2);
            if (Character.isDigit(par1) || par2 == 14) {
                statusTimeBox.textboxKeyTyped(par1, par2);
                jacobThresholdBox.textboxKeyTyped(par1, par2);
                jacobMushroomBox.textboxKeyTyped(par1, par2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateScreen() {
        super.updateScreen();
        urlTextBox.updateCursorCounter();
        statusTimeBox.updateCursorCounter();
        jacobThresholdBox.updateCursorCounter();
        jacobMushroomBox.updateCursorCounter();
    }

    protected void mouseClicked(int x, int y, int btn) {
        try {
            super.mouseClicked(x, y, btn);
            urlTextBox.mouseClicked(x, y, btn);
            statusTimeBox.mouseClicked(x, y, btn);
            jacobThresholdBox.mouseClicked(x, y, btn);
            jacobMushroomBox.mouseClicked(x, y, btn);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuiClosed() {
        Config.webhookUrl = urlTextBox.getText();
        Config.statusTime = Integer.parseInt(statusTimeBox.getText());
        Config.jacobThreshold = Integer.parseInt(jacobThresholdBox.getText());
        Config.jacobMushroom = Integer.parseInt(jacobMushroomBox.getText());
        Config.writeConfig();
    }
}
