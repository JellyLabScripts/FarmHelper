package com.jelly.farmhelperv2.gui;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;

/*
    Credits to Yuro for this superb class
*/
public class WelcomeGUI extends GuiScreen {
    static final int LIST_TOP_MARGIN = 80;
    static final int LIST_BOTTOM_MARGIN = 40;
    static final int OPEN_GUIDE_BUTTON_ID = 1;
    static final int CLOSE_BUTTON_ID = 2;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final String[] welcomeMessage = {
            "⚠ If you downloaded this mod from somewhere else than discord.gg/jellylab or GitHub ⚠",
            "⚠ such as YouTube videos, MediaFire, etc. - immediately remove it! It's a virus! ⚠",
            "",
            "Before you use, you NEED to: ",
            "1. Read the guide first! (Click \"Read the guide\").",
            "2. Build the farm using schematics provided in our guide.",
            "3. Join our Discord to get help and receive important information about the mod.",
            "4. For research purposes, we will collect statistics about your macro. You can opt out in the config.",
            "5. Type \"I understand\" in the box below and click \"Close\" to close this window.",
    };
    private GuiButton openGuideBtn;
    private GuiButton closeBtn;
    private GuiTextField textField;

    public WelcomeGUI() {
        super();
    }

    public static void showGUI() {
        if (!FarmHelperConfig.shownWelcomeGUI) {
            mc.displayGuiScreen(new WelcomeGUI());
        }
    }

    private void drawTitle() {
        float scale = 2;
        GL11.glScalef(scale, scale, 0.0F);
        Color chroma = Color.getHSBColor((float) ((System.currentTimeMillis() / 10) % 500) / 500, 1, 1);
        OneColor color = new OneColor(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 255);
        this.drawCenteredString(mc.fontRendererObj, "Welcome to Farm Helper!",
                (int) (this.width / 2f / scale), (int) (30 / scale),
                OneColor.HSBAtoARGB(color.getHue(), color.getSaturation(), color.getBrightness(), color.getAlpha()));
        GL11.glScalef(1.0F / scale, 1.0F / scale, 0.0F);
    }

    @Override
    public void initGui() {
        registerButtons();
        registerTextField();
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawBackground(0);
        drawTitle();
        float scale = this.width / 600f;
        int fontSize = (int) (this.width / 2f / scale);
        GL11.glScalef(scale, scale, 0.0F);
        for (int i = 0; i < welcomeMessage.length; i++) {
            String item = welcomeMessage[i];
            this.drawCenteredString(mc.fontRendererObj, item, (fontSize % 2 == 0 ? fontSize : fontSize - 1), (int) (this.height / 2f / scale - 80) + 16 * i, (i < 2 ? 0xFF0000 : 0xFFFFFF));
        }
        GL11.glScalef(1.0F / scale, 1.0F / scale, 0.0F);
        textField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void registerButtons() {
        addButton(OPEN_GUIDE_BUTTON_ID, this.width / 2 - 152, this.height - 30, "Read the guide");
        addButton(CLOSE_BUTTON_ID, this.width / 2 + 2, this.height - 30, "Close");
    }

    private void addButton(int id, int x, int y, String text) {
        GuiButton button = new GuiButton(id, x, y, 150, 20, text);
        this.buttonList.add(button);
        if (id == OPEN_GUIDE_BUTTON_ID) {
            openGuideBtn = button;
        } else if (id == CLOSE_BUTTON_ID) {
            closeBtn = button;
        }
    }

    private void registerTextField() {
        textField = new GuiTextField(0, mc.fontRendererObj, this.width / 2 - 100, this.height - 70, 200, 20);
        textField.setMaxStringLength(12);
        textField.setFocused(true);
        textField.setCanLoseFocus(false);
    }

    @Override
    public void updateScreen() {
        textField.updateCursorCounter();
        closeBtn.enabled = textField.getText().equalsIgnoreCase("I understand");
        super.updateScreen();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        textField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button == openGuideBtn) {
                try {
                    Desktop.getDesktop().browse(new java.net.URI("https://docs.google.com/document/d/1ji5J_eIan23zESPCglU8F4xxQgxLjvjuKgYBc-Pdvv8/edit?usp=sharing"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (button == closeBtn) {
                FarmHelperConfig.shownWelcomeGUI = true;
                mc.displayGuiScreen(new GuiMainMenu());
            }
        }
    }
}
