package com.jelly.farmhelperv2.gui;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jelly.farmhelperv2.FarmHelper;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.util.MarkdownFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    Credits to Yuro for this superb class
*/
public class AutoUpdaterGUI extends GuiScreen {
    static final int LIST_TOP_MARGIN = 80;
    static final int LIST_BOTTOM_MARGIN = 40;
    static final int DOWNLOAD_BUTTON_ID = 1;
    static final int CLOSE_BUTTON_ID = 2;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String GITHUB_API_URL = "https://api.github.com/repos/JellyLabScripts/FarmHelper/releases";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;
    public static boolean checkedForUpdates = false;
    public static boolean isOutdated = false;
    public static boolean shownGui = false;
    public static String latestVersion = "";
    private static List<String> releaseMessage = new ArrayList<>();
    private static final List<String> previousReleasesMessage = new ArrayList<>();
    private static List<String> splitReleaseMessage = new ArrayList<>();
    private static String downloadURL = null;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int displayGUI = 0;
    private int downloadProgress = 0;
    private GuiButton downloadBtn;
    private GuiButton closeBtn;
    private ScrollableList scrollableList;
    static Pattern versionPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+)(?:-(pre(\\d*))?)?");

    public AutoUpdaterGUI() {
        super();
    }

    public static void showGUI() {
        if (!shownGui) {
            shownGui = true;
            ScaledResolution res = new ScaledResolution(mc);
            splitReleaseMessage = splitLongStrings(releaseMessage, res.getScaledWidth() / 6);
            mc.displayGuiScreen(new AutoUpdaterGUI());
        }
    }

    public static void getLatestVersion() { // don't skid or i'll find you - yuro
        try {
            URL url = new URL(GITHUB_API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("Failed to fetch data from GitHub API. Response code: " + responseCode);
                return;
            }

            JsonArray releasesArray = getJsonElements(connection);
            JsonObject latestRelease = null;
            int releaseCount = 0;
            for (JsonElement release : releasesArray) {
                JsonObject releaseObject = release.getAsJsonObject();
                if (latestRelease == null || releaseObject.get("created_at").getAsString().compareTo(latestRelease.get("created_at").getAsString()) > 0) {
                    latestRelease = releaseObject;
                }
                if (releaseCount < 10) {
                    previousReleasesMessage.add("§3" + releaseObject.get("tag_name").getAsString() + "\n" + releaseObject.get("body").getAsString() + "\n");
                    releaseCount++;
                }
            }

            if (latestRelease == null) {
                System.out.println("No releases found for the repository.");
                return;
            }

            boolean isPreRelease = latestRelease.get("prerelease").getAsBoolean();
            String selectedReleaseMessage = "";
            if (isPreRelease && !FarmHelperConfig.autoUpdaterDownloadBetaVersions) {
                // If the latest version is pre-release and the user doesn't want to get pre-releases, find the latest release version
                for (JsonElement release : releasesArray) {
                    JsonObject releaseObject = release.getAsJsonObject();
                    if (!releaseObject.get("prerelease").getAsBoolean()) {
                        latestVersion = releaseObject.get("tag_name").getAsString();
                        System.out.println("Latest full release: " + latestVersion);
                        selectedReleaseMessage = releaseObject.get("body").getAsString();
                        downloadURL = releaseObject.get("assets").getAsJsonArray().get(0).getAsJsonObject().get("browser_download_url").getAsString();
                        break;
                    }
                }
            } else {
                // If the latest version is release or the user wants to get pre-release, get the latest version
                latestVersion = latestRelease.get("tag_name").getAsString();
                System.out.println("Latest release: " + latestVersion);
                selectedReleaseMessage = latestRelease.get("body").getAsString();
                downloadURL = latestRelease.get("assets").getAsJsonArray().get(0).getAsJsonObject().get("browser_download_url").getAsString();
            }

            isOutdated = isOutdated();

            if (!isOutdated) {
                System.out.println("Farm Helper is up to date!");
                return;
            }

            if (selectedReleaseMessage.isEmpty()) {
                releaseMessage = Arrays.asList("\nWell... We forgot to add something to the changelog. Oops. Just umm... update the mod I guess? \n \n".split("\n"));
                return;
            }
            selectedReleaseMessage += "\n\n§6Latest changelogs: \n\n" + String.join("\n", previousReleasesMessage);
            String cleanedMessage = "\n" + selectedReleaseMessage
                    .replaceAll("\r", "")
                    .replace("+ ", "§a+ ")
                    .replace("= ", "§7= ")
                    .replace("\n- ", "\n§c- ")
                    .replace("Changelog:", "")
                    + " \n \n"; // fix for top and bottom padding
            releaseMessage = Arrays.asList(cleanedMessage.split("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JsonArray getJsonElements(HttpURLConnection connection) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(response.toString());
        return jsonElement.getAsJsonArray();
    }

    private static List<String> splitLongStrings(List<String> inputList, int maxLength) {
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
        Matcher latestVersionMatcher = versionPattern.matcher(latestVersion);
        Matcher currentVersionMatcher = versionPattern.matcher(FarmHelper.VERSION);

        if (!latestVersionMatcher.find() || latestVersionMatcher.group(1) == null || latestVersionMatcher.group(1).isEmpty()) {
            System.out.println("Failed to parse latest version.");
            return false;
        }
        if (!currentVersionMatcher.find() || currentVersionMatcher.group(1) == null || currentVersionMatcher.group(1).isEmpty()) {
            System.out.println("Failed to parse current version.");
            return false;
        }
        int latestPre = 0; // -pre version
        if (latestVersionMatcher.group(2) == null || latestVersionMatcher.group(2).isEmpty())
            latestPre = 1337; // without -pre version (full release)
        if (latestVersionMatcher.group(3) != null && !latestVersionMatcher.group(3).isEmpty())
            latestPre = Integer.parseInt(latestVersionMatcher.group(3)); // -preX version

        int currentPre = 0; // -pre version
        if (currentVersionMatcher.group(2) == null || currentVersionMatcher.group(2).isEmpty())
            currentPre = 1337; // without -pre version (full release)
        if (currentVersionMatcher.group(3) != null && !currentVersionMatcher.group(3).isEmpty())
            currentPre = Integer.parseInt(currentVersionMatcher.group(3)); // -preX version

        if (compareSymVer(latestVersionMatcher.group(1)) > 0)
            return true;
        else if (compareSymVer(latestVersionMatcher.group(1)) == 0)
            return latestPre > currentPre;

        return false;
    }

    private static int compareSymVer(String latestVersion) {
        Matcher currentMatcher = versionPattern.matcher(FarmHelper.VERSION);

        if (!currentMatcher.find()) {
            return 0;
        }

        String[] parts1 = latestVersion.split("\\.");
        String[] parts2 = currentMatcher.group(1).split("\\.");

        for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
            int part1 = Integer.parseInt(parts1[i]);
            int part2 = Integer.parseInt(parts2[i]);

            if (part1 < part2) {
                return -1;
            } else if (part1 > part2) {
                return 1;
            }
        }

        if (parts1.length < parts2.length) {
            return -1;
        } else if (parts1.length > parts2.length) {
            return 1;
        }

        return 0;
    }

    private static void deleteAndClose() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            File currentModsFolder = FarmHelper.jarFile.getParentFile();
            File[] filesToDelete = currentModsFolder.listFiles(
                    (dir, name) -> name.toLowerCase().startsWith("farmhelper") && !name.toLowerCase().contains("jda") && !name.toLowerCase().contains(latestVersion));
            if (filesToDelete != null) {
                for (File fileToDelete : filesToDelete) {
                    if (SystemUtils.IS_OS_WINDOWS) {
                        try {
                            Runtime.getRuntime().exec("cmd /c ping 0 -n 2 & del \"" + fileToDelete.getAbsolutePath() + "\"");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        fileToDelete.deleteOnExit();
                    }
                }
            }
        }, "FarmHelperV2-Delete-Old-Files"));
        mc.shutdown();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBackgroundAndContent(mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBackgroundAndContent(int mouseX, int mouseY, float partialTicks) {
        this.drawBackground(0);

        scrollableList.drawScreen(mouseX, mouseY, partialTicks);

        switch (displayGUI) {
            case 0:
                drawOutdatedMessage();
                break;
            case 1:
                drawUpdateInProgressMessage();
                break;
            case 2:
                drawUpdateCompletedMessage();
                break;
            case 3:
                drawJavaErrorMessage();
                break;
        }

        ScaledResolution res = new ScaledResolution(mc);
        this.drawString(mc.fontRendererObj, "What's new? (" + latestVersion + ") ➤", (int) (res.getScaledHeight() / 8f),
                LIST_TOP_MARGIN - 12, Color.GREEN.getRGB());
    }

    private void drawOutdatedMessage() {
        float scale = 2;
        GL11.glScalef(scale, scale, 0.0F);
        this.drawCenteredString(mc.fontRendererObj, "Outdated version of Farm Helper", (int) (this.width / 2f / scale),
                (int) (30 / scale), Color.RED.darker().getRGB());
        GL11.glScalef(1.0F / scale, 1.0F / scale, 0.0F);
    }

    private void drawUpdateInProgressMessage() {
        float scale = 2;
        GL11.glScalef(scale, scale, 0.0F);
        Color chroma = Color.getHSBColor((float) ((System.currentTimeMillis() / 10) % 500) / 500, 1, 1);
        OneColor color = new OneColor(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 255);
        this.drawCenteredString(mc.fontRendererObj, "Updating Farm Helper... (" + downloadProgress + "%) Please wait",
                (int) (this.width / 2f / scale), (int) (30 / scale),
                OneColor.HSBAtoARGB(color.getHue(), color.getSaturation(), color.getBrightness(), color.getAlpha()));
        GL11.glScalef(1.0F / scale, 1.0F / scale, 0.0F);
    }

    private void drawUpdateCompletedMessage() {
        float scale = 2;
        GL11.glScalef(scale, scale, 0.0F);
        this.drawCenteredString(mc.fontRendererObj, "Farm Helper has been updated!", (int) (this.width / 2f / scale),
                (int) (30 / scale), Color.GREEN.getRGB());
        GL11.glScalef(1.0F / scale, 1.0F / scale, 0.0F);
    }

    private void drawJavaErrorMessage() {
        float scale = 2;
        GL11.glScalef(scale, scale, 0.0F);
        this.drawCenteredString(mc.fontRendererObj, "Error: Your Java is extremely outdated!", (int) (this.width / 2f / scale), (int) (20 / scale), Color.RED.darker().getRGB());
        this.drawCenteredString(mc.fontRendererObj, "Please, update Java and try again.", (int) (this.width / 2f / scale), (int) (40 / scale), Color.RED.darker().getRGB());
        GL11.glScalef(1.0F / scale, 1.0F / scale, 0.0F);
    }

    @Override
    public void initGui() {
        ScaledResolution res = new ScaledResolution(mc);
        scrollableList = new ScrollableList(res.getScaledWidth(), res.getScaledHeight(), LIST_TOP_MARGIN,
                res.getScaledHeight() - LIST_BOTTOM_MARGIN, 12);
        scrollableList.registerScrollButtons(7, 8);
        registerButtons();
        super.initGui();
    }

    private void registerButtons() {
        addButton(DOWNLOAD_BUTTON_ID, this.width / 2 - 152, this.height - 30, "Download new version");
        addButton(CLOSE_BUTTON_ID, this.width / 2 + 2, this.height - 30, "Close");
    }

    private void addButton(int id, int x, int y, String text) {
        GuiButton button = new GuiButton(id, x, y, 150, 20, text);
        this.buttonList.add(button);
        if (id == DOWNLOAD_BUTTON_ID) {
            downloadBtn = button;
        } else if (id == CLOSE_BUTTON_ID) {
            closeBtn = button;
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
        splitReleaseMessage = splitLongStrings(releaseMessage, res.getScaledWidth() / 6);
        super.updateScreen();
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button.enabled) {
            switch (button.id) {
                case DOWNLOAD_BUTTON_ID:
                    if (displayGUI != 0) return;
                    if (latestVersion == null || latestVersion.isEmpty()) return;
                    executor.submit(() -> {
                        try {
                            downloadBtn.enabled = false;
                            closeBtn.enabled = false;
                            displayGUI = 1;
                            downloadFileWithProgress(
                                    downloadURL,
                                    new File(mc.mcDataDir + "/mods/FarmHelperV2-" + latestVersion + ".jar")
                            );
                            mc.addScheduledTask(() -> {
                                downloadBtn.enabled = false;
                                closeBtn.enabled = true;
                                displayGUI = 2;
                            });
                        } catch (javax.net.ssl.SSLHandshakeException e) {
                            downloadBtn.enabled = false;
                            closeBtn.enabled = true;
                            displayGUI = 3;
                        } catch (IOException e) {
                            downloadBtn.enabled = true;
                            closeBtn.enabled = true;
                            displayGUI = 0;
                            e.printStackTrace();
                        }
                    });
                    break;
                case CLOSE_BUTTON_ID:
                    if (displayGUI == 2)
                        deleteAndClose();
                    else
                        mc.displayGuiScreen(new GuiMainMenu());
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

    private void downloadFileWithProgress(String downloadURL, File outputFile) throws IOException {
        URL url = new URL(downloadURL);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(AutoUpdaterGUI.CONNECT_TIMEOUT);
        connection.setReadTimeout(AutoUpdaterGUI.READ_TIMEOUT);

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

    private class ScrollableList extends GuiSlot { // spent way too much time on this - yuro
        private ScrollableList(int width, int height, int top, int bottom, int slotHeight) {
            super(AutoUpdaterGUI.mc, width, height, top, bottom, slotHeight);
        }

        @Override
        protected int getSize() {
            return splitReleaseMessage.size();
        }

        @Override
        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
        }

        @Override
        protected boolean isSelected(int index) {
            return false;
        }

        @Override
        protected void drawBackground() {
        }

        @Override
        protected void drawSlot(int entryID, int insideLeft, int yPos, int insideSlotHeight, int mouseXIn, int mouseYIn) {
            if (entryID >= 0 && entryID < splitReleaseMessage.size()) {
                String item = MarkdownFormatter.format(splitReleaseMessage.get(entryID));
                ScaledResolution res = new ScaledResolution(mc);
                drawString(mc.fontRendererObj, item, (int) (res.getScaledHeight() / 8f), yPos + 2, Color.WHITE.getRGB());
            }
        }

        @Override
        protected int getContentHeight() {
            return getSize() * slotHeight;
        }

        @Override
        protected int getScrollBarX() {
            ScaledResolution res = new ScaledResolution(mc);
            return res.getScaledWidth() - 6;
        }
    }
}
