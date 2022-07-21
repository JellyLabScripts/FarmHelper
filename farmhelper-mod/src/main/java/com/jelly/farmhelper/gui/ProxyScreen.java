package com.jelly.farmhelper.gui;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.enums.ProxyType;
import com.jelly.farmhelper.config.interfaces.ProxyConfig;
import com.jelly.farmhelper.gui.components.GuiCheckBox;
import com.jelly.farmhelper.network.proxy.ConnectionState;
import com.jelly.farmhelper.network.proxy.ProxyManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.awt.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ProxyScreen extends GuiScreen {
    public static ConnectionState state;
    private static String stateString;
    GuiTextField proxyAddressTf;
    GuiTextField proxyUsernameTf;
    GuiTextField proxyPasswordTf;
    GuiScreen parent;
    GuiButton typeBtn;
    GuiButton connectBtn;
    GuiButton disconnectBtn;
    GuiButton closeGuiBtn;
    GuiCheckBox connectAtStartupCb;
    public ProxyScreen (final GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawBackground(0);
        this.proxyAddressTf.drawTextBox();
        this.proxyUsernameTf.drawTextBox();
        this.proxyPasswordTf.drawTextBox();

        if (connectAtStartupCb.isChecked() && state == ConnectionState.INVALID) {
            this.connectAtStartupCb.setIsChecked(false);
            ConfigHandler.set("connectAtStartup", this.connectAtStartupCb.isChecked());
            connectAtStartupCb.enabled = false;
        } else {
            connectAtStartupCb.enabled = true;
        }
        if (state == ConnectionState.CONNECTED) {
            this.toggleButtons(false);
            this.toggleTextFields(false);
            this.disconnectBtn.enabled = true;
            this.closeGuiBtn.enabled = true;
        } else if (state == ConnectionState.CONNECTING) {
            toggleTextFields(false);
            this.toggleButtons(false);
            this.disconnectBtn.enabled = true;
        } else {
            toggleTextFields(true);
            this.toggleButtons(state != ConnectionState.INVALID);
            this.closeGuiBtn.enabled = true;
        }

        if (this.proxyAddressTf.getText().isEmpty() && !this.proxyAddressTf.isFocused()) {
            this.proxyAddressTf.setText(ProxyConfig.address);
            validateProxy();
        }

        if (this.proxyUsernameTf.getText().isEmpty() && !this.proxyUsernameTf.isFocused())
            this.proxyUsernameTf.setText(ProxyConfig.username);
        if (this.proxyPasswordTf.getText().isEmpty() && !this.proxyPasswordTf.isFocused())
            this.proxyPasswordTf.setText(ProxyConfig.password);

        this.drawString(this.fontRendererObj, "IP:PORT ➤", this.width / 2 - 123, this.height / 2 - 85, Color.GRAY.getRGB());

        this.drawHorizontalLine(this.width / 2 - 145, this.width / 2 + 145, this.height / 2 - 40, Color.DARK_GRAY.getRGB());

        this.drawCenteredString(this.fontRendererObj, state.color + stateString, this.width / 2, this.height / 2 - 25, Color.WHITE.getRGB());

        this.drawCenteredString(this.fontRendererObj, "(OPTIONAL) ProxyManager authentication", this.width / 2, this.height / 2 - 5, Color.WHITE.getRGB());

        this.drawString(this.fontRendererObj, "USERNAME ➤", this.width / 2 - 135, this.height / 2 + 25, Color.GRAY.getRGB());

        this.drawString(this.fontRendererObj, "PASSWORD ➤", this.width / 2 - 135, this.height / 2 + 55, Color.GRAY.getRGB());

        super.drawScreen(mouseX, mouseY, partialTicks);
    }


    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        this.proxyAddressTf.textboxKeyTyped(typedChar, keyCode);
        this.proxyUsernameTf.textboxKeyTyped(typedChar, keyCode);
        this.proxyPasswordTf.textboxKeyTyped(typedChar, keyCode);

        ConfigHandler.set("proxyAddress", this.proxyAddressTf.getText());
        ConfigHandler.set("proxyUsername", this.proxyUsernameTf.getText());
        ConfigHandler.set("proxyPassword", this.proxyPasswordTf.getText());

        if (this.proxyAddressTf.isFocused()) {
            validateProxy();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.proxyAddressTf.mouseClicked(mouseX, mouseY, mouseButton);
        this.proxyUsernameTf.mouseClicked(mouseX, mouseY, mouseButton);
        this.proxyPasswordTf.mouseClicked(mouseX, mouseY, mouseButton);
    }


    @Override
    public void initGui() {
        super.initGui();
        if (state == null) {
            state = ConnectionState.DISCONNECTED;
        }
        if (stateString == null) {
            setStateString("Disconnected");
        }
        this.proxyAddressTf = new GuiTextField(6, this.fontRendererObj, this.width / 2 - 70, this.height / 2 - 90, 140, 20);
        this.proxyAddressTf.setMaxStringLength(25);

        this.proxyUsernameTf = new GuiTextField(7, this.fontRendererObj, this.width / 2 - 70, this.height / 2 + 20, 140, 20);
        this.proxyUsernameTf.setMaxStringLength(256);

        this.proxyPasswordTf = new GuiTextField(8, this.fontRendererObj, this.width / 2 - 70, this.height / 2 + 50, 140, 20);
        this.proxyPasswordTf.setMaxStringLength(256);

        registerButtons();
    }

    private void registerButtons() {
        this.typeBtn = new GuiButton(1, this.width / 2 + 80, this.height / 2 - 90, 60, 20, ProxyConfig.type.toString());
        buttonList.add(this.typeBtn);

        this.connectAtStartupCb = new GuiCheckBox(2, this.width / 2 + 80, this.height / 2 + 35, "Connect at startup", 15, 15, ProxyConfig.connectAtStartup);
        buttonList.add(this.connectAtStartupCb);

        this.connectBtn = new GuiButton(3, this.width / 2 - 95, this.height / 2 + 85, 61, 20, "Connect");
        buttonList.add(this.connectBtn);

        this.disconnectBtn = new GuiButton(4, this.width / 2 - 30, this.height / 2 + 85, 61, 20, "Disconnect");
        buttonList.add(this.disconnectBtn);

        this.closeGuiBtn = new GuiButton(5, this.width / 2 + 35, this.height / 2 + 85, 61, 20, "Close");
        buttonList.add(this.closeGuiBtn);
    }


    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 1: // typeBtn
                long type = (long) ConfigHandler.get("proxyType");
                type = type == 0 ? 1 : 0;
                this.proxyPasswordTf.setEnabled(type == 1);
                ConfigHandler.set("proxyType", type);
                button.displayString = String.valueOf(ProxyConfig.type);
                break;
            case 2: // connect at startup checkbox
                this.connectAtStartupCb.setIsChecked(this.connectAtStartupCb.isChecked());
                ConfigHandler.set("connectAtStartup", this.connectAtStartupCb.isChecked());
                break;
            case 3: // connect btn
                state = ConnectionState.CONNECTING;
                setStateString("Connecting...");
                ProxyManager.testProxy();
                break;
            case 4: // disconnect btn
                state = ConnectionState.DISCONNECTED;
                setStateString("Disconnected");
                break;
            case 5: // close gui btn
                mc.displayGuiScreen(parent);
                break;
        }
    }

    @Override
    public void updateScreen() {
        this.proxyAddressTf.updateCursorCounter();
        this.proxyUsernameTf.updateCursorCounter();
        this.proxyPasswordTf.updateCursorCounter();
    }

    private void validateProxy() {
        String proxyaddress = this.proxyAddressTf.getText();
        Matcher matcher = Pattern.compile("\\b(25[0-5]|2[0-4]\\d|1\\d\\d|\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|\\d{1,2})){3}:([1-9]\\d{0,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])\\b").matcher(proxyaddress);
        if (matcher.matches()) {
            this.proxyAddressTf.setTextColor(Color.GREEN.getRGB());
            if (state == ConnectionState.INVALID) {
                state = ConnectionState.DISCONNECTED;
                setStateString("Disconnected");
                toggleButtons(true);
            }
        } else {
            this.proxyAddressTf.setTextColor(Color.RED.getRGB());
            toggleButtons(false);
            // We still want them to be able to change type of socket
            this.typeBtn.enabled = true;
            state = ConnectionState.INVALID;
            setStateString("Invalid IP/PORT");
        }
    }

    public static void setStateString(String s) {
        stateString = s;
    }

    private void toggleButtons(boolean toggle) {
        this.connectBtn.enabled = toggle;
        this.disconnectBtn.enabled = toggle;
        this.typeBtn.enabled = toggle;
    }

    private void toggleTextFields(boolean toggle) {
        this.proxyAddressTf.setEnabled(toggle);
        this.proxyUsernameTf.setEnabled(toggle);
        this.proxyPasswordTf.setEnabled(ProxyConfig.type != ProxyType.SOCKS4 && toggle);
    }
}
