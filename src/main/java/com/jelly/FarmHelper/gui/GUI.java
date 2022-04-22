package com.jelly.FarmHelper.gui;

import com.jelly.FarmHelper.FarmHelper;
import com.jelly.FarmHelper.config.Config;
import com.jelly.FarmHelper.config.CropEnum;
import com.jelly.FarmHelper.config.FarmEnum;
import com.jelly.FarmHelper.gui.buttons.GuiBetterButton;
import com.jelly.FarmHelper.gui.buttons.GuiCustomButton;
import com.jelly.FarmHelper.gui.buttons.GuiCustomSwitchButton;
import com.jelly.FarmHelper.utils.Utils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GUI extends GuiScreen{
    int buttonWidth = 115;
    int buttonHeight = 40;
    private static final ResourceLocation quarterI = new ResourceLocation(FarmHelper.MODID, "textures/gui/a.png");
    private static final ResourceLocation quarterII = new ResourceLocation(FarmHelper.MODID, "textures/gui/b.png");
    private static final ResourceLocation quarterIII = new ResourceLocation(FarmHelper.MODID, "textures/gui/c.png");
    private static final ResourceLocation quarterIV = new ResourceLocation(FarmHelper.MODID, "textures/gui/d.png");

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.add(new GuiCustomButton(100, this.width/2,  this.height/2 - 100, 0,0,  quarterI));
        this.buttonList.add(new GuiCustomButton(1, this.width/2 - 100 ,  this.height / 2 - 200, 100, 100, quarterI));
        this.buttonList.add(new GuiCustomButton(2, this.width/2,  this.height / 2 - 200, 100,100,  quarterII));
        this.buttonList.add(new GuiCustomButton(3, this.width/2 - 100 ,  this.height / 2 - 100, 100,100,  quarterIII));
        this.buttonList.add(new GuiCustomButton(4, this.width/2 ,  this.height / 2 - 100, 100,100,  quarterIV));
        this.buttonList.add(new GuiBetterButton(5, this.width / 2 - buttonWidth / 2, this.height / 2 - buttonHeight / 2 + 60, buttonWidth, buttonHeight, "Settings"));
        this.buttonList.add(new GuiBetterButton(6, this.width / 2 - buttonWidth / 2, this.height / 2 - buttonHeight / 2 + 110, buttonWidth, buttonHeight, "Toggle Profit GUI"));
        this.buttonList.add(new GuiBetterButton(7, this.width / 2 - buttonWidth / 2, this.height / 2 - buttonHeight / 2 + 160, buttonWidth, buttonHeight, Config.FarmType.name()));
        this.buttonList.add(new GuiBetterButton(8, this.width / 2 - buttonWidth / 2, this.height / 2 - buttonHeight / 2 + 210, buttonWidth, buttonHeight, "Sell Inventory"));
        // this.buttonList.add(new GuiBetterButton(9, this.width / 2 - buttonWidth / 2 - 200, this.height / 2 - buttonHeight / 2 + 210, buttonWidth, buttonHeight, "Test Button"));

        GuiCustomButton temp = (GuiCustomButton) this.buttonList.get(Config.CropType.ordinal());
        temp.select();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 225).getRGB());
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame(){
        return false;
    }

    void deactivateOthers(int currentButtonIndex){
        for (int i = 0; i < 4; i++){
            if (i != currentButtonIndex) {
                GuiCustomButton others = (GuiCustomButton) this.buttonList.get(i);
                others.setDeactivate();
            }
        }
    }


    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
             GuiCustomButton temp = (GuiCustomButton) this.buttonList.get(0);
             temp.select();
             deactivateOthers(0);
             Config.CropType = CropEnum.WHEAT;
            Config.writeConfig();
        }
        if (button.id == 2) {
            GuiCustomButton temp = (GuiCustomButton) this.buttonList.get(1);
            temp.select();
            deactivateOthers(1);
            Config.CropType = CropEnum.NETHERWART;
            Config.writeConfig();
        }
        if (button.id == 3) {
            GuiCustomButton temp = (GuiCustomButton) this.buttonList.get(2);
            temp.select();
            deactivateOthers(2);
            Config.CropType = CropEnum.POTATO;
            Config.writeConfig();
        }
        if (button.id == 4) {
            GuiCustomButton temp = (GuiCustomButton) this.buttonList.get(3);
            temp.select();
            deactivateOthers(3);
            Config.CropType = CropEnum.CARROT;
            Config.writeConfig();
        }
        if (button.id == 5) {
            mc.thePlayer.closeScreen();
            mc.displayGuiScreen(new GuiSettings());
        }
        if (button.id == 6) {
            FarmHelper.profitGUI = !FarmHelper.profitGUI;
        }
        if (button.id == 7) {
            Config.FarmType = FarmEnum.values()[1 - Config.FarmType.ordinal()];
            GuiBetterButton temp = (GuiBetterButton) button;
            temp.displayString = Config.FarmType.name();
            updateScreen();
        }
        if (button.id == 8) {
            FarmHelper.openedGUI = false;
            mc.thePlayer.closeScreen();
            FarmHelper.cookie = true;
            Utils.ExecuteRunnable(FarmHelper.checkFooter);
            if (FarmHelper.cookie) Utils.debugLog("Executing sell in 1 second");
            Utils.ScheduleRunnable(FarmHelper.autoSell, 1, TimeUnit.SECONDS);
        }
        if (button.id == 9) {
            FarmHelper.openedGUI = false;
            mc.thePlayer.closeScreen();
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    FarmHelper.sellInventory();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            // Utils.ScheduleRunnable(FarmHelper.buyCookie, 2, TimeUnit.SECONDS);
        }
    }
}
