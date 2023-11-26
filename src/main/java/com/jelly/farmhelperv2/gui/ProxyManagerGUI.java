package com.jelly.farmhelperv2.gui;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.impl.Proxy;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class ProxyManagerGUI extends GuiScreen {

    private final GuiScreen parent;

    private GuiButton enableButton;
    private GuiTextField hostField;
    private GuiButton typeButton;
    private GuiTextField usernameField;
    private GuiTextField passwordField;

    private GuiButton saveButton;
    private GuiButton cancelButton;

    public ProxyManagerGUI(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        enableButton = new GuiButton(0, width / 2 - 100, height / 2 - 100, 200, 20, "Enable proxy: " + (FarmHelperConfig.proxyEnabled ? "§a" : "§c") + FarmHelperConfig.proxyEnabled);

        typeButton = new GuiButton(1, width / 2 - 100, height / 2 - 75, 200, 20, "Type: " + FarmHelperConfig.proxyType);

        hostField = new GuiTextField(2, fontRendererObj, width / 2 - 100, height / 2 - 50, 200, 20);
        hostField.setMaxStringLength(100);
        hostField.setText(FarmHelperConfig.proxyAddress);

        usernameField = new GuiTextField(3, fontRendererObj, width / 2 - 100, height / 2, 200, 20);
        usernameField.setMaxStringLength(100);
        usernameField.setText(FarmHelperConfig.proxyUsername);

        passwordField = new GuiTextField(4, fontRendererObj, width / 2 - 100, height / 2 + 25, 200, 20);
        passwordField.setMaxStringLength(100);
        passwordField.setText(FarmHelperConfig.proxyPassword);

        saveButton = new GuiButton(5, width / 2 - 100, height / 2 + 75, 80, 20, "Save");
        cancelButton = new GuiButton(6, width / 2 + 20, height / 2 + 75, 80, 20, "Cancel");

        this.buttonList.add(enableButton);
        this.buttonList.add(typeButton);
        this.buttonList.add(saveButton);
        this.buttonList.add(cancelButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        drawCenteredString(fontRendererObj, "Proxy Manager", width / 2, height / 2 - 115, 0xffffff);

        hostField.drawTextBox();
        if (hostField.getText().isEmpty() && !hostField.isFocused()) {
            hostField.setText("IP:PORT");
        } else if (hostField.getText().equals("IP:PORT") && hostField.isFocused()) {
            hostField.setText("");
        }

        usernameField.drawTextBox();

        if (usernameField.getText().isEmpty() && !usernameField.isFocused()) {
            usernameField.setText("Username");
        } else if (usernameField.getText().equals("Username") && usernameField.isFocused()) {
            usernameField.setText("");
        }

        passwordField.drawTextBox();
        if (passwordField.getText().isEmpty() && !passwordField.isFocused()) {
            passwordField.setText("Password");
        } else if (passwordField.getText().equals("Password") && passwordField.isFocused()) {
            passwordField.setText("");
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            FarmHelperConfig.proxyEnabled = !FarmHelperConfig.proxyEnabled;
            enableButton.displayString = "Enable proxy: " + (FarmHelperConfig.proxyEnabled ? "§a" : "§c") + FarmHelperConfig.proxyEnabled;
        } else if (button.id == 1) {
            if (FarmHelperConfig.proxyType == Proxy.ProxyType.SOCKS) {
                FarmHelperConfig.proxyType = Proxy.ProxyType.HTTP;
            } else {
                FarmHelperConfig.proxyType = Proxy.ProxyType.SOCKS;
            }
            typeButton.displayString = "Type: " + FarmHelperConfig.proxyType;
        } else if (button.id == 5) {
            Proxy.getInstance().setProxy(FarmHelperConfig.proxyEnabled, hostField.getText(), FarmHelperConfig.proxyType, usernameField.getText(), passwordField.getText());
            mc.displayGuiScreen(parent);
        } else if (button.id == 6) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
        }
        if (hostField.isFocused()) {
            hostField.textboxKeyTyped(typedChar, keyCode);
        }
        if (usernameField.isFocused()) {
            usernameField.textboxKeyTyped(typedChar, keyCode);
        }
        if (passwordField.isFocused()) {
            passwordField.textboxKeyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        hostField.mouseClicked(mouseX, mouseY, mouseButton);
        usernameField.mouseClicked(mouseX, mouseY, mouseButton);
        passwordField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        this.hostField.updateCursorCounter();
        this.usernameField.updateCursorCounter();
        this.passwordField.updateCursorCounter();
        super.updateScreen();
    }
}
