package FarmHelper.gui.buttons;

import FarmHelper.gui.GuiSettings;
import FarmHelper.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

import java.awt.*;

public class GuiCustomSwitchButton extends GuiButton {

    int x; int y; int widthln; int length; boolean selected = false; String buttonText; int wordX; int wordY;

    GuiSettings guiSettings = new GuiSettings();
    public GuiCustomSwitchButton(int buttonId, int x, int y, int wordX, int wordY,int widthln, int length, String buttonText) {

        super(buttonId, x, y, widthln, length, buttonText);
        this.wordX = wordX;
        this.wordY = wordY;
        this.x = x;
        this.y = y;
        this.widthln = widthln;
        this.length = length;
        this.buttonText = buttonText;
    }


    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        int color = -1;
        if (this.selected) {
            color = new Color(255, 80, 80).getRGB();
            Gui.drawRect(x + widthln / 2, y, x + widthln, y + length, color);
        }
        else {
            color = new Color(242, 242, 242).getRGB();
            Gui.drawRect(x, y, x + widthln/2, y + length, color);
        }
        Utils.drawString(buttonText,  wordX, wordY,  1, -1);
        Gui.drawRect(x, y, x + widthln, y + length, 0x30000000);
        Utils.drawHorizontalLine(x - 1, x+widthln, y - 1, color);
        Utils.drawHorizontalLine(x - 1, x+widthln, y + length, color);
        Utils.drawVerticalLine(x - 1, y - 1, y + length,  color);
        Utils.drawVerticalLine(x + widthln, y - 1, y + length, color);
    }

    public void switchSelect() {
        selected = !selected;
    }
}
