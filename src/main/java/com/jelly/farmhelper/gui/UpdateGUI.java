package com.jelly.farmhelper.gui;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.jelly.farmhelper.network.APIHelper;
import com.jelly.farmhelper.utils.MarkdownFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import org.apache.commons.io.FileUtils;
import com.google.gson.JsonObject;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final int LIST_TOP_MARGIN = 80;
    private final int LIST_BOTTOM_MARGIN = 40;
    private static List<String> message = new ArrayList<>();
    static List<String> releaseMessage;
    private ScrollableList scrollableList;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) { // don't skid or i'll find you - yuro
        this.drawBackground(0);
        scrollableList.drawScreen(mouseX, mouseY, partialTicks);
        float scale = 2;
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
        ScaledResolution res = new ScaledResolution(mc);
        this.drawString(mc.fontRendererObj, "What's new? ➤", (int) (res.getScaledHeight() / 8f), LIST_TOP_MARGIN - 12, Color.GREEN.getRGB());
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void initGui() {
        ScaledResolution res = new ScaledResolution(mc);
        scrollableList = new ScrollableList(res.getScaledWidth(), res.getScaledHeight(), LIST_TOP_MARGIN, res.getScaledHeight() - LIST_BOTTOM_MARGIN, 12);
        scrollableList.registerScrollButtons(7, 8);
        registerButtons();
        super.initGui();
    }

    private class ScrollableList extends GuiSlot { // spent way too much time on this - yuro
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
                String item = MarkdownFormatter.format(message.get(entryID));
                ScaledResolution res = new ScaledResolution(mc);
                drawString(mc.fontRendererObj, item, (int) (res.getScaledHeight() / 8f), yPos + 2, Color.WHITE.getRGB());
            }
        }

        @Override
        protected int getContentHeight() { return getSize() * slotHeight; }

        @Override
        protected int getScrollBarX() {
            ScaledResolution res = new ScaledResolution(mc);
            return res.getScaledWidth() - 6;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);

        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(new GuiMainMenu());
        }
    }

    @Override
    public void updateScreen() {
        ScaledResolution res = new ScaledResolution(mc);
        message = splitLongStrings(releaseMessage, res.getScaledWidth() / 6);
        super.updateScreen();
    }

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
                            // FileUtils.copyURLToFile(new URL("https://github.com/JellyLabScripts/FarmHelper/releases/download/" + latestVersion + "/FarmHelper-" + latestVersion + ".jar"), new File(mc.mcDataDir + "/mods/farmhelper-mod-" + latestVersion + ".jar"), CONNECT_TIMEOUT, READ_TIMEOUT);
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
                                File fileToDelete = new File(mc.mcDataDir + "/mods/FarmHelper-v" + VERSION + ".jar");
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
            shownGui = true;
            outdated = isOutdated();
            releaseMessage = Arrays.asList(getReleaseMessage().replaceAll("\r", "").replace("+ ", "§a+ ").replace("= ", "§f= ").replace("- ", "§c- ").split("\n"));
            ScaledResolution res = new ScaledResolution(mc);
            message = splitLongStrings(releaseMessage, res.getScaledWidth() / 6);
            mc.displayGuiScreen(new UpdateGUI());
        }
    }

    public static List<String> splitLongStrings(List<String> inputList, int maxLength) {
        List<String> splitStrings = new ArrayList<>();
        if (inputList == null || inputList.isEmpty()) return splitStrings;

        for (String message : inputList) {
            if (message.length() <= maxLength) {
                splitStrings.add(message);
            } else {
                int endIndex = findLastSpaceBeforeIndex(message, maxLength);
                splitStrings.add(message.substring(0, endIndex).trim());
                splitStrings.addAll(splitLongStrings(
                        createListWithSingleElement(message.substring(endIndex).trim()), maxLength)
                );
            }
        }

        return splitStrings;
    }

    // Helper method to find the last space before a given index 'maxIndex'
    private static int findLastSpaceBeforeIndex(String str, int maxIndex) {
        for (int i = maxIndex - 1; i >= 0; i--) {
            if (str.charAt(i) == ' ') {
                return i;
            }
        }
        return maxIndex;
    }

    // Helper method to create a new ArrayList with a single element
    private static List<String> createListWithSingleElement(String element) {
        List<String> list = new ArrayList<>();
        list.add(element);
        return list;
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
            JsonObject j = APIHelper.readJsonFromUrl("https://api.github.com/repos/JellyLabScripts/FarmHelper/releases/latest",
                            "User-Agent",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
            return "\n" + j.get("body").toString() + " \n \n"; // fix for top and bottom padding
        } catch (Exception e) {
            return "No release message was found.";
        }
    }
}
