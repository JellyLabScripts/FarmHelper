package com.jelly.farmhelper.gui;

import com.jelly.farmhelper.network.APIHelper;
import com.jelly.farmhelper.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.json.simple.JSONObject;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;

import static com.jelly.farmhelper.FarmHelper.MODVERSION;

public class UpdateGUI extends GuiScreen {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean shownGui = false;
    public static boolean outdated = false;
    private GuiButton downloadBtn;
    private static String[] message;
    private GuiButton closeBtn;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawBackground(0);
        float scale = 3;
        GL11.glScalef(scale, scale, 0.0F);
        this.drawCenteredString(mc.fontRendererObj, "Outdated version of FarmHelper", (int) (this.width / 2f / scale), (int) (this.height / 6f / scale), Color.RED.darker().getRGB());
        GL11.glScalef(1.0F / scale, 1.0F / scale, 0.0F);
        scale = 1.5f;
        GL11.glScalef(scale, scale, 0.0F);
        this.drawString(mc.fontRendererObj, "What's new? ➤", (int) (this.width / 2f / scale - 180), (int) (this.height / 6 / scale + 25), Color.GREEN.getRGB());
        GL11.glScalef(1.0F / scale, 1.0F / scale, 0.0F);
        if (message != null) {
            int y = 40;
            for (String s : message) {
                this.drawString(mc.fontRendererObj, s, this.width / 2 - 160, this.height / 6 + y, Color.WHITE.getRGB());
                y += 15;
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void initGui() {
        super.initGui();
        registerButtons();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);

        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(new GuiMainMenu());
        }
    }

    @Override
    protected void actionPerformed(GuiButton button)  {
        switch (button.id) {
            case 1: // closebtn
                mc.displayGuiScreen(new GuiMainMenu());
                break;
            case 2: // downloadbtn
                Utils.openURL("https://github.com/Jelly298/FarmHelper/releases/latest");
                mc.displayGuiScreen(new GuiMainMenu());

                break;
        }
    }

    private void registerButtons() {
        this.closeBtn = new GuiButton(1, this.width / 2, this.height / 2 + 100, 150, 20, "Close");
        this.buttonList.add(this.closeBtn);

        this.downloadBtn = new GuiButton(2, this.width / 2 - 150, this.height / 2 + 100, 150, 20, "Download new version");
        this.buttonList.add(this.downloadBtn);
    }

    public static void showGUI() {
        if (!shownGui && isOutdated()) {
            mc.displayGuiScreen(new UpdateGUI());
            shownGui = true;
            outdated = isOutdated();
            message = getReleaseMessage().replaceAll("\r", "").replace("+ ", "§a+ ").replace("= ", "§f= ").replace("- ", "§c- ").split("\n");
        }
    }
    private static boolean isOutdated() {
        try {
            String latestversion = APIHelper.readJsonFromUrl("https://api.github.com/repos/Jelly298/FarmHelper/releases/latest",
                            "User-Agent",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36")
                    .get("tag_name").toString();
            return !MODVERSION.contains(latestversion) && !MODVERSION.contains("PRE");
        } catch (Exception e) {
            return false;
        }
    }

    private static String getReleaseMessage() {
        try {
            JSONObject j = APIHelper.readJsonFromUrl("https://api.github.com/repos/Jelly298/FarmHelper/releases/latest",
                            "User-Agent",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
            return j.get("body").toString();
        } catch (Exception e) {
            return "No release message was found.";
        }
    }

}
