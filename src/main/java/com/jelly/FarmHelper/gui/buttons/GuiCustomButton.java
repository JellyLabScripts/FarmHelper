package com.jelly.FarmHelper.gui.buttons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiCustomButton extends GuiButton {



    ResourceLocation Rl;
    int x; int y; int widthln; int length; public boolean selected = false;

    public GuiCustomButton(int buttonId, int x, int y, int widthln, int length, ResourceLocation rl) {
        super(buttonId, x, y, widthln, length, "");
        this.Rl = rl;
        this.x = x;
        this.y = y;
        this.widthln = widthln;
        this.length = length;

    }


    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {

            mc.getTextureManager().bindTexture(Rl);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 1, 1, widthln, height, widthln, height);

            if(this.selected)
            GlStateManager.color(1.0F, 0.2F, 0.2F);
            else
                GlStateManager.color(1.0F, 1.0F, 1F);

    }

    public void select(){
        selected = !selected;
    }
    public void setDeactivate(){
        selected = false;
    }
}
