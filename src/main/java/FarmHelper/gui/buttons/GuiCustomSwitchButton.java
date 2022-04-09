package FarmHelper.gui.buttons;

import FarmHelper.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

public class GuiCustomSwitchButton extends GuiButton {

    int x; int y; int widthln; int length; boolean selected = false; String buttonText; int distanceFromLeft;

    public GuiCustomSwitchButton(int buttonId, int x, int y, int widthln, int length, int distanceFromLeft, String buttonText) {
        super(buttonId, x, y, widthln, length, buttonText);
        this.x = x;
        this.y = y;
        this.widthln = widthln;
        this.length = length;
        this.buttonText = buttonText;
        this.distanceFromLeft = distanceFromLeft;


    }


    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {

        int color = -1;

        if(this.selected)
        {
            color = 0x9000FF00;
            Gui.drawRect(x + widthln / 2, y, x + widthln, y + length, color);
        }
        else {
            color = 0x90F2F2F2;
            Gui.drawRect(x, y, x + widthln/2, y + length, color);
        }

        Gui.drawRect(x, y, x + widthln, y + length, 0x30000000);
        Utils.drawHorizontalLine(x - 1, x+widthln, y - 1, color);
        Utils.drawHorizontalLine(x - 1, x+widthln, y + length, color);
        Utils.drawVerticalLine(x - 1, y - 1, y + length,  color);
        Utils.drawVerticalLine(x + widthln, y - 1, y + length, color);

        mc.fontRendererObj.drawString(buttonText, distanceFromLeft, y + length/2 - 4, -1);

    }
    public void switchSelect(){selected = !selected;}
}
