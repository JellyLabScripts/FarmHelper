package com.jelly.farmhelper.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.client.config.GuiUtils;

public class GuiCheckBox extends GuiButton {
    private boolean isChecked;
    private final int boxWidth;

    public GuiCheckBox(int id, int xPos, int yPos, String displayString, int boxWidth, int height, boolean isChecked) {
        super(id, xPos, yPos, displayString);
        this.isChecked = isChecked;
        this.boxWidth = boxWidth;
        this.height = height;
        this.width = this.boxWidth + 2 + Minecraft.getMinecraft().fontRendererObj.getStringWidth(displayString);
    }

    /**
     * Draws this button to the screen.
     */
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY)
    {
        if (this.visible)
        {
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.boxWidth && mouseY < this.yPosition + this.height;
            GuiUtils.drawContinuousTexturedBox(buttonTextures, this.xPosition, this.yPosition, 0, 46, this.boxWidth, this.height, 200, 20, 2, 3, 2, 2, this.zLevel);
            this.mouseDragged(mc, mouseX, mouseY);
            int color = 14737632;

            if (packedFGColour != 0)
            {
                color = packedFGColour;
            }
            else if (!this.enabled)
            {
                color = 10526880;
            }

            if (this.isChecked)
                this.drawCenteredString(mc.fontRendererObj, "x", this.xPosition + this.boxWidth / 2 + 1, this.yPosition + this.height / 2 - 4, 14737632);

            this.drawString(mc.fontRendererObj, displayString, xPosition + this.boxWidth + 2, yPosition + this.height / 2 - 3, color);
        }
    }

    /**
     * Returns true if the mouse has been pressed on this control. Equivalent of MouseListener.mousePressed(MouseEvent
     * e).
     */
    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
    {
        if (this.enabled && this.visible && mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height)
        {
            this.isChecked = !this.isChecked;
            return true;
        }

        return false;
    }

    public boolean isChecked()
    {
        return this.isChecked;
    }

    public void setIsChecked(boolean isChecked)
    {
        this.isChecked = isChecked;
    }
}
