package FarmHelper.GUI.buttons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiCustomSwitchButton extends GuiButton {

    int x; int y; int widthln; int length; public boolean selected = false; String buttonText;

    public GuiCustomSwitchButton(int buttonId, int x, int y, int widthln, int length, String buttonText) {
        super(buttonId, x, y, widthln, length, buttonText);
        this.x = x;
        this.y = y;
        this.widthln = widthln;
        this.length = length;
        this.buttonText = buttonText;

    }


    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {

        Gui.drawRect(x, y, x + widthln, y + length, 0x30000000);
        if(this.selected)
        {
            Gui.drawRect(x + widthln / 2, y, x + widthln, y + length, 0x9000FF00);
        }
        else {
            Gui.drawRect(x, y, x + widthln / 2, y + length, 0x90000000);
        }
        mc.fontRendererObj.drawString(buttonText, x - mc.fontRendererObj.getStringWidth(buttonText) - 4, y + length/2 - 4, 0x90000000);

    }
    public void switchSelect(){selected = !selected;}
}
