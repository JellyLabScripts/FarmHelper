package FarmHelper.GUI;

import FarmHelper.FarmHelper;
import FarmHelper.GUI.buttons.GuiBetterButton;
import FarmHelper.GUI.buttons.GuiCustomButton;
import FarmHelper.GUI.buttons.GuiCustomSwitchButton;
import FarmHelper.Utils.Utils;
import FarmHelper.config.Config;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;

public class GuiSettings extends GuiScreen {

    String title = "Settings";


    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {



    }
    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.add(new GuiCustomSwitchButton(0, this.width/2 + 100, this.height/2 - 50, 100, 30, 30, "Rotate after teleport"));
        this.buttonList.add(new GuiCustomSwitchButton(1, this.width/2 + 100, this.height/2     , 100, 30, 30, "Inventory price calculator"));
        this.buttonList.add(new GuiCustomSwitchButton(2, this.width/2 + 100, this.height/2 + 50, 100, 30, 30, "Profit calculator"));
        initialSelect();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, this.width, this.height, new Color(41, 41, 41, 255).getRGB());
        Utils.drawString(title,this.width / 2 - mc.fontRendererObj.getStringWidth(title) / 2 * 2, 30, 2, -1); // multiply by the size smh works
        super.drawScreen(mouseX, mouseY, partialTicks);

    }

    @Override
    public boolean doesGuiPauseGame(){
        return false;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {

        if(button.id == 0){
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) button;
            temp.switchSelect();
            Config.rotateAfterTeleport = !Config.rotateAfterTeleport;
            updateScreen();
        }
        if(button.id == 1){
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) button;
            temp.switchSelect();
            Config.inventoryPriceCalculator = !Config.inventoryPriceCalculator;
            updateScreen();
        }
        if(button.id == 2){
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) button;
            temp.switchSelect();
            Config.profitCalculator = !Config.profitCalculator;
            updateScreen();
        }

    }

    void initialSelect(){
        if(Config.rotateAfterTeleport) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(0);
            temp.switchSelect();
        }
        if(Config.inventoryPriceCalculator) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(1);
            temp.switchSelect();
        }
        if(Config.profitCalculator) {
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(2);
            temp.switchSelect();
        }
    }





}
