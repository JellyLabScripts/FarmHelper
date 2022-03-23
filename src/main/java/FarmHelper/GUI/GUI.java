package FarmHelper.GUI;

import FarmHelper.FarmHelper;
import FarmHelper.GUI.buttons.GuiCustomButton;
import FarmHelper.GUI.buttons.GuiCustomSwitchButton;
import FarmHelper.Utils.Utils;
import FarmHelper.config.Config;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

import java.io.IOException;
import java.io.Serializable;

public class GUI extends GuiScreen implements Serializable {


    int buttonWidth = 85;
    int buttonHeight = 65;

    private static final ResourceLocation quarterI = new ResourceLocation(FarmHelper.MODID, "textures/gui/a.png");
    private static final ResourceLocation quarterII = new ResourceLocation(FarmHelper.MODID, "textures/gui/b.png");
    private static final ResourceLocation quarterIII = new ResourceLocation(FarmHelper.MODID, "textures/gui/c.png");
    private static final ResourceLocation quarterIV = new ResourceLocation(FarmHelper.MODID, "textures/gui/d.png");

    public static boolean changeAngleAfterCycle = false;
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {



    }
    @Override
    public void initGui() {
        super.initGui();


        this.buttonList.add(new GuiCustomButton(100, this.width/2,  this.height/2, 0,0,  quarterI));

        this.buttonList.add(new GuiCustomButton(1, this.width/2 - 100 ,  this.height / 2 - 100, 100, 100, quarterI));
        this.buttonList.add(new GuiCustomButton(2, this.width/2,  this.height / 2 - 100, 100,100,  quarterII));
        this.buttonList.add(new GuiCustomButton(3, this.width/2 - 100 ,  this.height / 2, 100,100,  quarterIII));
        this.buttonList.add(new GuiCustomButton(4, this.width/2 ,  this.height / 2, 100,100,  quarterIV));

        this.buttonList.add(new GuiButtonExt(6, this.width / 2 - buttonWidth / 2 - 180, this.height / 2 - buttonHeight / 2, buttonWidth, buttonHeight, "Change angle"));

        this.buttonList.add(new GuiButtonExt(7, this.width / 2 - buttonWidth / 2  + 180, this.height / 2 - buttonHeight / 2, buttonWidth, buttonHeight, "Change farm"));
        this.buttonList.add(new GuiButtonExt(8, this.width / 2 - buttonWidth / 2  + 180, this.height / 2 - buttonHeight / 2 + 80, 160, 40, "Rotate after teleport"));
       // this.buttonList.add(new GuiCustomSwitchButton(9, this.width / 2 - buttonWidth / 2  + 220, this.height / 2 - buttonHeight / 2 - 80, 80, 20, "Rotate after teleport"));

        GuiCustomButton temp = (GuiCustomButton) this.buttonList.get(FarmHelper.config.CropType.ordinal());
        temp.select();

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawRect(0, 0, this.width, this.height, 0x30000000);
    }

    @Override
    public boolean doesGuiPauseGame(){
        return false;
    }

    void deactivateOthers(int currentButtonIndex){
        for (int i = 0; i < 4; i++){
            if(i != currentButtonIndex) {
                GuiCustomButton others = (GuiCustomButton) this.buttonList.get(i);
                others.setDeactivate();
            }
        }
    }


    @Override
    protected void actionPerformed(GuiButton button) throws IOException {

        if(button.id == 1){

             GuiCustomButton temp = (GuiCustomButton) this.buttonList.get(0);
             temp.select();
             deactivateOthers(0);
             FarmHelper.config.CropType = Config.CROP.WHEAT;
        }
        if(button.id == 2){

            GuiCustomButton temp = (GuiCustomButton) this.buttonList.get(1);
            temp.select();
            deactivateOthers(1);
            FarmHelper.config.CropType = Config.CROP.NETHERWART;
        }
        if(button.id == 3){
            GuiCustomButton temp = (GuiCustomButton) this.buttonList.get(2);
            temp.select();
            deactivateOthers(2);
            FarmHelper.config.CropType = Config.CROP.POTATO;
        }if(button.id == 4){
            GuiCustomButton temp = (GuiCustomButton) this.buttonList.get(3);
            temp.select();
            deactivateOthers(3);
            FarmHelper.config.CropType = Config.CROP.CARROT;
        }



        if(button.id == 6){
            if(FarmHelper.config.Angle.equals(Config.ANGLE.A0)){
                FarmHelper.config.Angle = Config.ANGLE.A90;
            } else if(FarmHelper.config.Angle.equals(Config.ANGLE.A90)){
                FarmHelper.config.Angle = Config.ANGLE.A180;
            } else if(FarmHelper.config.Angle.equals(Config.ANGLE.A180)){
                FarmHelper.config.Angle = Config.ANGLE.AN90;
            } else{
                System.out.println("hi");
                FarmHelper.config.Angle = Config.ANGLE.A0;
            }


        }
        if(button.id == 7){
            FarmHelper.config.FarmType =
                    FarmHelper.config.FarmType.equals(Config.FARM.VERTICAL)? Config.FARM.LAYERED : Config.FARM.VERTICAL;

            Utils.saveConfig(FarmHelper.config);
        }
        if(button.id == 8){
            changeAngleAfterCycle = !changeAngleAfterCycle;
            updateScreen();

            Utils.saveConfig(FarmHelper.config);
        }
       /* if(button.id == 9){
            System.out.println("pressed");
            GuiCustomSwitchButton temp = (GuiCustomSwitchButton) this.buttonList.get(8);
            temp.switchSelect();

            updateScreen();

            Utils.saveConfig(FarmHelper.config);
        }*/



    }






}
