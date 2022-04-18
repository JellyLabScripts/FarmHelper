package FarmHelper.gui;

import FarmHelper.config.Config;
import FarmHelper.gui.buttons.GuiCustomSwitchButton;
import FarmHelper.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

import java.awt.*;
import java.io.IOException;

public class GuiSettings extends GuiScreen {
    String title = "Settings";
    int firstY = 80;
    int gap = 30;
    int fieldWidth = 200;
    int fieldHeight = 20;

    private GuiTextField urlTextBox;

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) { }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.add(new GuiCustomSwitchButton(0, this.width/2 + 120, firstY, this.width/2 - 150, firstY + 2, 40, 15, "Auto ReSync"));
        this.buttonList.add(new GuiCustomSwitchButton(1, this.width/2 + 120, firstY + gap, this.width/2 - 150, firstY + gap + 2, 40, 15, "Debug Mode"));
        this.buttonList.add(new GuiCustomSwitchButton(2, this.width/2 + 120, firstY + gap * 2, this.width/2 - 150, firstY + gap * 2 + 2, 40, 15, "Compact Debug"));
        this.buttonList.add(new GuiCustomSwitchButton(3, this.width/2 + 120, firstY + gap * 3, this.width/2 - 150, firstY + gap * 3 + 2, 40, 15, "Webhook Log"));
        urlTextBox = new GuiTextField(1, Minecraft.getMinecraft().fontRendererObj, this.width/2 + 160 - fieldWidth, firstY + gap * 4, fieldWidth, fieldHeight);
        // urlTextBox = new GuiTextField(1, Minecraft.getMinecraft().fontRendererObj, this.width/2 - fieldWidth / 2 + 30 , this.height / 2 - fieldHeight / 2 + 60, fieldWidth, fieldHeight);
        urlTextBox.setMaxStringLength(256);
        urlTextBox.setText(Config.webhookUrl);
        urlTextBox.setFocused(true);

        // this.buttonList.add(new GuiCustomSwitchButton(0, this.width/2 + 120, firstY, this.width/2 - 150, firstY + 2, 40, 15,"Rotate after teleport"));
        // this.buttonList.add(new GuiCustomSwitchButton(1, this.width/2 + 120, firstY + gap,this.width/2 - 150, firstY + gap + 2, 40, 15, "Inventory price calculator"));
        // this.buttonList.add(new GuiCustomSwitchButton(2, this.width/2 + 120, firstY + gap * 2,this.width/2 - 150, firstY + gap * 2 + 2, 40, 15 , "Profit calculator"));
        // this.buttonList.add(new GuiCustomSwitchButton(3, this.width/2 + 120, firstY + gap * 3,this.width/2 - 150, firstY + gap * 3 + 2,40, 15 , "Auto resync"));
        // this.buttonList.add(new GuiCustomSwitchButton(4, this.width/2 + 120, firstY + gap * 4,this.width/2 - 150, firstY + gap * 4 + 2, 40, 15, "Autosell (make sure you have cookies on!!)"));
        // this.buttonList.add(new GuiCustomSwitchButton(5, this.width/2 + 120, this.height/2 + 140, 40, 15, 30, "Fastbreak"));

        initialSelect();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, this.width, this.height, new Color(41, 41, 41, 255).getRGB());
        Utils.drawString(title, this.width / 2 - mc.fontRendererObj.getStringWidth(title) / 2 * 2, 30, 2, -1); // multiply by the size smh works firstY + gap * 3,this.width/2 - 150
        mc.fontRendererObj.drawStringWithShadow("Webhook URL", this.width/2 - 150, firstY + gap * 4 + 2, -1);
        urlTextBox.drawTextBox();
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
            if (Config.debug && Config.compactDebug) {
                actionPerformed(buttonList.get(2));
            }
            Config.debug = !Config.debug;
            updateScreen();
            Config.writeConfig();
        }
        else if (button.id == 2) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) button;
            if (Config.debug) {
                Config.compactDebug = !Config.compactDebug;
                temp.switchSelect();
            }
            updateScreen();
            Config.writeConfig();
        }
        else if (button.id == 3) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) button;
            temp.switchSelect();
            Config.webhookLog = !Config.webhookLog;
            updateScreen();
            Config.writeConfig();
        }
    }

    void initialSelect() {
        if (Config.resync) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(0);
            temp.switchSelect();
        }
        if (Config.debug) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(1);
            temp.switchSelect();
        }
        if (Config.compactDebug) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(2);
            temp.switchSelect();
        }
        if (Config.webhookLog) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(3);
            temp.switchSelect();
        }
    }

    protected void keyTyped(char par1, int par2) {
        try {
            super.keyTyped(par1, par2);
            urlTextBox.textboxKeyTyped(par1, par2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateScreen() {
        super.updateScreen();
        urlTextBox.updateCursorCounter();
    }

    protected void mouseClicked(int x, int y, int btn) {
        try {
            super.mouseClicked(x, y, btn);
            urlTextBox.mouseClicked(x, y, btn);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuiClosed(){
        Config.webhookUrl = urlTextBox.getText();
        Config.writeConfig();
    }
}
