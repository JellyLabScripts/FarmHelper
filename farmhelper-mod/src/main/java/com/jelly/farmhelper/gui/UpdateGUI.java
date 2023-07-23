package com.jelly.farmhelper.gui;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.jelly.farmhelper.network.APIHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import net.minecraft.client.gui.GuiSlot;

import static com.jelly.farmhelper.FarmHelper.MODVERSION;
import static com.jelly.farmhelper.FarmHelper.VERSION;

public class UpdateGUI extends GuiScreen {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean shownGui = false;
    public static boolean outdated = false;
    private GuiButton downloadBtn;
    private GuiButton closeBtn;
    static String latestVersion = "";
    private int displayGUI = 0;
    private int downloadProgress = 0;
    protected OneColor color = new OneColor(0, 0, 0, 255);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static int GUI_WIDTH = mc.displayWidth;
    private static final int GUI_HEIGHT = mc.displayHeight / mc.gameSettings.guiScale - 160;
    private static final int LIST_TOP_MARGIN = 160 / mc.gameSettings.guiScale;
    private static final int LIST_BOTTOM_MARGIN = 80 / mc.gameSettings.guiScale;
    private static List<String> message = new ArrayList<>();
    private ScrollableList scrollableList;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) { // don't skid or i'll find you - yuro
        this.drawBackground(0);
        scrollableList.drawScreen(mouseX, mouseY, partialTicks);
        float scale = mc.gameSettings.guiScale;
        GL11.glScalef(scale, scale, 0.0F);
        if (displayGUI == 0)
            this.drawCenteredString(mc.fontRendererObj, "Outdated version of FarmHelper", (int) (this.width / 2f / scale), (int) (30 / scale), Color.RED.darker().getRGB());
        if (displayGUI == 1) {
            Color chroma = Color.getHSBColor((float) ((System.currentTimeMillis() / 10) % 500) / 500, 1, 1);
            color.setFromOneColor(new OneColor(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 255));
            this.drawCenteredString(mc.fontRendererObj, "Updating FarmHelper... (" + downloadProgress + "%) Please wait", (int) (this.width / 2f / scale), (int) (30 / scale), OneColor.HSBAtoARGB(color.getHue(), color.getSaturation(), color.getBrightness(), color.getAlpha()));
        }
        if (displayGUI == 2)
            this.drawCenteredString(mc.fontRendererObj, "FarmHelper has been updated!", (int) (this.width / 2f / scale), (int) (30 / scale), Color.GREEN.getRGB());
        GL11.glScalef(1.0F / scale, 1.0F / scale, 0.0F);
        scale = 1f;
        GL11.glScalef(scale, scale, 0.0F);
        this.drawString(mc.fontRendererObj, "What's new? ➤", (int) (this.width / 8f / scale), GUI_HEIGHT / mc.gameSettings.guiScale + 25, Color.GREEN.getRGB());
        GL11.glScalef(1.0F / scale, 1.0F / scale, 0.0F);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void initGui() {
        super.initGui();
        GUI_WIDTH = mc.displayWidth / mc.gameSettings.guiScale; // fix for changing window size
        scrollableList = new ScrollableList(GUI_WIDTH, GUI_HEIGHT, LIST_TOP_MARGIN, height - LIST_BOTTOM_MARGIN, 10);
        scrollableList.registerScrollButtons(7, 8);
        registerButtons();
    }

    private class ScrollableList extends GuiSlot {
        private ScrollableList(int width, int height, int top, int bottom, int slotHeight) {
            super(UpdateGUI.mc, width, height, top, bottom, slotHeight);
        }

        @Override
        protected int getSize() {
            return message.size();
        }

        @Override
        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {}

        @Override
        protected boolean isSelected(int index) { return false; }

        @Override
        protected void drawBackground() {}

        @Override
        protected void drawSlot(int entryID, int insideLeft, int yPos, int insideSlotHeight, int mouseXIn, int mouseYIn) {
            if (entryID >= 0 && entryID < message.size()) {
                String item = message.get(entryID);
                drawString(mc.fontRendererObj, item, (int) (mc.displayWidth / 8f / mc.gameSettings.guiScale), yPos + 2, Color.WHITE.getRGB());
            }
        }

        @Override
        protected int getContentHeight() { return getSize() * slotHeight; }

        @Override
        protected int getScrollBarX() { return GUI_WIDTH - 12 / mc.gameSettings.guiScale; }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);

        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(new GuiMainMenu());
        }
    }

    @Override
    public void updateScreen() { super.updateScreen(); }

    @Override
    protected void actionPerformed(GuiButton button)  {
        if (button.enabled) {
            switch (button.id) {
                case 1: // downloadBtn
                    if (displayGUI != 0) return;
                    if (latestVersion == null || latestVersion.isEmpty()) return;
                    executor.submit(() -> {
                        int CONNECT_TIMEOUT = 10000;
                        int READ_TIMEOUT = 10000;
                        try {
                            downloadBtn.enabled = false;
                            closeBtn.enabled = false;
                            displayGUI = 1;
                            //FileUtils.copyURLToFile(new URL("https://github.com/JellyLabScripts/FarmHelper/releases/download/" + latestVersion + "/FarmHelper-" + latestVersion + ".jar"), new File(mc.mcDataDir + "/mods/farmhelper-mod-" + latestVersion + ".jar"), CONNECT_TIMEOUT, READ_TIMEOUT);
                            downloadFileWithProgress(("https://github.com/JellyLabScripts/FarmHelper/releases/download/" + latestVersion + "/FarmHelper-" + latestVersion + ".jar"), new File(mc.mcDataDir + "/mods/farmhelper-mod-" + latestVersion + ".jar"), CONNECT_TIMEOUT, READ_TIMEOUT);
                            mc.addScheduledTask(() -> {
                                downloadBtn.enabled = false;
                                closeBtn.enabled = true;
                                displayGUI = 2;
                            });
                        } catch (IOException e) {
                            downloadBtn.enabled = true;
                            closeBtn.enabled = true;
                            displayGUI = 0;
                            e.printStackTrace();
                        }
                    });
                    break;
                case 2: // closeBtn
                    if (displayGUI == 0) mc.displayGuiScreen(new GuiMainMenu());
                    if (displayGUI == 2) {
                        mc.addScheduledTask(() -> {
                            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                                File fileToDelete = new File(mc.mcDataDir + "/mods/farmhelper-mod-v" + VERSION + ".jar");
                                if (fileToDelete.exists()) {
                                    try {
                                        FileUtils.forceDelete(fileToDelete);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }));
                        });
                        mc.shutdown();
                    }
                    break;
            }
        }
        scrollableList.actionPerformed(button);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        scrollableList.handleMouseInput();
    }

    private void downloadFileWithProgress(String downloadURL, File outputFile, int connectTimeout, int readTimeout) throws IOException {
        URL url = new URL(downloadURL);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        int fileSize = connection.getContentLength();
        int downloadedBytes = 0;

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;
                downloadProgress = (int) ((downloadedBytes / (double) fileSize) * 100);
            }
        }
    }

    private void registerButtons() {
        this.downloadBtn = new GuiButton(1, this.width / 2 - 152, this.height - 30, 150, 20, "Download new version");
        this.buttonList.add(this.downloadBtn);
        this.closeBtn = new GuiButton(2, this.width / 2 + 2, this.height - 30, 150, 20, "Close");
        this.buttonList.add(this.closeBtn);
    }

    public static void showGUI() {
        if (!shownGui && isOutdated()) {
            mc.displayGuiScreen(new UpdateGUI());
            shownGui = true;
            outdated = isOutdated();
            message = Arrays.asList(getReleaseMessage().replaceAll("\r", "").replace("+ ", "§a+ ").replace("= ", "§f= ").replace("- ", "§c- ").split("\n"));
        }
    }
    private static boolean isOutdated() {
        try {
            latestVersion = APIHelper.readJsonFromUrl("https://api.github.com/repos/JellyLabScripts/FarmHelper/releases/latest",
                            "User-Agent",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36")
                    .get("tag_name").toString();
            return (!MODVERSION.toLowerCase().contains(latestVersion.toLowerCase()) && !VERSION.toLowerCase().contains("pre"));
        } catch (Exception e) {
            return false;
        }
    }

    private static String getReleaseMessage() {
        try {
            JSONObject j = APIHelper.readJsonFromUrl("https://api.github.com/repos/JellyLabScripts/FarmHelper/releases/latest",
                            "User-Agent",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
            return "\n" + j.get("body").toString() + "\n\n"; // fix for top and bottom padding
        } catch (Exception e) {
            return "No release message was found.";
        }
    }
}
