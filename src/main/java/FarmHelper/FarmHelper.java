package FarmHelper;

import FarmHelper.GUI.GUI;
import FarmHelper.Utils.Utils;
import FarmHelper.config.AngleEnum;
import FarmHelper.config.Config;
import FarmHelper.config.CropEnum;
import FarmHelper.config.FarmEnum;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import scala.tools.nsc.Global;

import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod(modid = FarmHelper.MODID, name = FarmHelper.NAME, version = FarmHelper.VERSION)
public class FarmHelper implements Serializable
{
    Minecraft mc = Minecraft.getMinecraft();

    public static final String MODID = "nwmath";
    public static final String NAME = "Farm Helper";
    public static final String VERSION = "1.0";

    private static final ResourceLocation mutantNetherwartImage = new ResourceLocation(FarmHelper.MODID, "textures/gui/mnw.png");
    private static final ResourceLocation enchantedNetherwartImage = new ResourceLocation(FarmHelper.MODID, "textures/gui/enw.png");


    public static boolean enabled = false;


    boolean locked = false;
    boolean process1 = false;
    boolean process2 = false;
    boolean process3 = false;
    boolean process4 = false;
    boolean error = false;
    boolean emergency = false;
    boolean setspawned = false;
    boolean setAntiStuck = false;
    boolean set = false; //whether HAS CHANGED motion (1&2)
    boolean set3 = false; //same but motion 3
    boolean checkedPrice = false;


    double beforeX = 0;
    double beforeZ = 0;
    double deltaX = 10000;
    double deltaZ = 10000;
    double initialX = 0;
    double initialZ = 0;


    boolean notInIsland = false;
    boolean shdBePressingKey = true;
    public static boolean openedGUI = false;

    public int keybindA = mc.gameSettings.keyBindLeft.getKeyCode();
    public int keybindD = mc.gameSettings.keyBindRight.getKeyCode();
    public int keybindW = mc.gameSettings.keyBindForward.getKeyCode();
    public int keybindS = mc.gameSettings.keyBindBack.getKeyCode();
    public int keybindAttack = mc.gameSettings.keyBindAttack.getKeyCode();


    static volatile int totalMnw = 0;
    static volatile int totalEnw = 0;
    static volatile int totalMoney = 0;
    static volatile int prevMoney = -999;
    int angleMode= 0 ;
    static volatile int moneyper10sec = 0;

    MouseHelper mouseHelper = new MouseHelper();




    private static Logger logger;


    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {

    }


    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {

        MinecraftForge.EVENT_BUS.register(new FarmHelper());
        MinecraftForge.EVENT_BUS.register(new GUI());
    }



    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        if(Config.CropType == null) {
            System.out.print("not read");
            Config.setConfig(CropEnum.NETHERWART, FarmEnum.LAYERED, AngleEnum.AN90);
        }
        ScheduleRunnable(checkPriceChange, 1, TimeUnit.SECONDS);


    }


    /*@SubscribeEvent
    public void onDisconnect(GuiDisconnected disconnected) throws RuntimeException{

        ScheduledExecutorService executor1 = Executors.newScheduledThreadPool(1);
        executor1.schedule(Reconnect, 3, TimeUnit.SECONDS);
    }*/


    @SubscribeEvent
    public void onMessageReceived(ClientChatReceivedEvent event){

        if(event.message.getFormattedText().contains("You were spawned in Limbo") && !notInIsland && enabled) {
           activateFailsafe();
            ScheduledExecutorService executor1 = Executors.newScheduledThreadPool(1);
            executor1.schedule(LeaveSBIsand, 8, TimeUnit.SECONDS);

        }
        if((event.message.getFormattedText().contains("Sending to server") && !notInIsland && enabled)){
            activateFailsafe();
            ScheduledExecutorService executor1 = Executors.newScheduledThreadPool(1);
            executor1.schedule(WarpHome, 10, TimeUnit.SECONDS);
        }
        if((event.message.getFormattedText().contains("DYNAMIC") && notInIsland)){
            error = true;
        }
        if((event.message.getFormattedText().contains("SkyBlock Lobby") && !notInIsland && enabled)){
            activateFailsafe();
            ScheduledExecutorService executor1 = Executors.newScheduledThreadPool(1);
            executor1.schedule(LeaveSBIsand, 10, TimeUnit.SECONDS);

        }
        if((event.message.getFormattedText().contains("Warped from") && !notInIsland && enabled && Config.rotateAfterTeleport)){
            angleMode = 1 - angleMode;
        }

    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void render(RenderGameOverlayEvent event)
    {

        if (event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            mc.fontRendererObj.drawString(Config.CropType + "", 4, 4, -1);
            mc.fontRendererObj.drawString("Angle : " + angleToValue(Config.Angle), 4, 16, -1);
            mc.fontRendererObj.drawString(Config.FarmType + " farm", 4, 28, -1);
           // mc.fontRendererObj.drawString( "Change angle after teleport : " + Config.rotateAfterTeleport, 4, 40, -1);

            if(Config.CropType.equals(CropEnum.NETHERWART) && Config.inventoryPriceCalculator) {
                mc.getTextureManager().bindTexture(mutantNetherwartImage);
                GuiScreen.drawModalRectWithCustomSizedTexture(4, 50, 1, 1, 12, 12, 12, 12);
                mc.getTextureManager().bindTexture(enchantedNetherwartImage);
                GuiScreen.drawModalRectWithCustomSizedTexture(4, 62, 1, 1, 12, 12, 12, 12);
                Utils.drawString("x" + totalMnw, 20, 50, 0.8f, -1);
                Utils.drawString("x" + totalEnw, 20, 62, 0.8f, -1);
                Utils.drawString("$" + totalMoney, 6, 78, 0.95f, -1);
            }
            if(Config.CropType.equals(CropEnum.NETHERWART) && Config.profitCalculator) {
                Utils.drawString("profit/min = " + moneyper10sec * 6, 6, 94, 0.8f, -1);
                Utils.drawString("profit/h = " + moneyper10sec * 6 * 60, 6, 104, 0.8f, -1);
            }

        }

    }



    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void OnTickPlayer(TickEvent.ClientTickEvent event) {



        if (event.phase != TickEvent.Phase.START) return;

        // profit calculator
        if( mc.thePlayer != null && mc.theWorld != null){


            int tempEnw = 0; int tempMnw = 0;
            for (int i = 0; i < 35; i++) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if(stack != null) {
                    if (stack.getDisplayName().contains("Enchanted Nether Wart"))//
                        tempEnw = tempEnw + stack.stackSize;

                    if (stack.getDisplayName().contains("Mutant Nether Wart"))
                        tempMnw = tempMnw + stack.stackSize;
                }

            }
            totalMnw = tempMnw; totalEnw = tempEnw;
            totalMoney = totalMnw * 51504 + totalEnw * 320;

        }

        //script code
        if (enabled && mc.thePlayer != null && mc.theWorld != null) {

            //always
            mc.gameSettings.pauseOnLostFocus = false;
            mc.thePlayer.inventory.currentItem = 0;
            mc.gameSettings.gammaSetting = 100;



            if(!emergency) {
                KeyBinding.setKeyBindState(keybindW, false);
                KeyBinding.setKeyBindState(keybindS, false);
            }
            if (!shdBePressingKey) {
                KeyBinding.setKeyBindState(keybindA, false);
                KeyBinding.setKeyBindState(keybindD, false);
            }
            //angles
            if(!emergency) {
                if (Config.CropType.equals(CropEnum.NETHERWART)) {
                    mc.thePlayer.rotationPitch = 0;

                } else {
                    mc.thePlayer.rotationPitch = 6;
                }

                if(angleMode == 0){
                    mc.thePlayer.rotationYaw = mc.thePlayer.rotationYaw - mc.thePlayer.rotationYaw % 360 + angleToValue(Config.Angle);
                } else {
                    mc.thePlayer.rotationYaw = mc.thePlayer.rotationYaw - mc.thePlayer.rotationYaw % 360 + angleToValue(Config.Angle) + 180;
                }
            }
            //INITIAL SETUP
            if (!locked) {
                KeyBinding.setKeyBindState(keybindA, false);
                KeyBinding.setKeyBindState(keybindD, false);
                locked = true;
                angleMode = 0;
                initialize();
                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN +
                                "[Farm Helper] : " + EnumChatFormatting.DARK_GREEN + "Starting script"));

                ScheduleRunnable(checkChange, 3, TimeUnit.SECONDS);
            }
            //antistuck
            if(!setAntiStuck && Config.FarmType.equals(FarmEnum.LAYERED)){
                if (playerBlock() == Blocks.farmland || playerBlock() == Blocks.soul_sand){

                    mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN +
                            "[Farm Helper] : " + EnumChatFormatting.DARK_GREEN + "Detected stuck"));

                    setAntiStuck = true;
                    process4 = true;
                    ScheduleRunnable(stopAntistuck, 800, TimeUnit.MILLISECONDS);
                }

            }
            if(deltaX < 0.8d && deltaZ < 0.8d && !notInIsland && !emergency && !setAntiStuck && Config.FarmType.equals(FarmEnum.LAYERED)){

                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN +
                        "[Farm Helper] : " + EnumChatFormatting.DARK_GREEN + "Detected stuck"));
                setAntiStuck = true;
                process4 = true;
                ScheduleRunnable(stopAntistuck, 800, TimeUnit.MILLISECONDS);

            }else if(deltaX < 0.1d && deltaZ < 0.1d && !notInIsland && !emergency && !setAntiStuck && Config.FarmType.equals(FarmEnum.VERTICAL)){
                 //tp pad fix
                deltaX = 10000;
                deltaZ = 10000;
                ScheduleRunnable(changeMotion, 800, TimeUnit.MILLISECONDS);
            }



            //bedrock failsafe
            Block blockStandingOn = mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ)).getBlock();
            if(blockStandingOn == Blocks.bedrock && !emergency) {

                KeyBinding.setKeyBindState(keybindAttack, false);
                process1 = false;
                process2 = false;
                process3 = false;
                process4 = false;
                ScheduleRunnable(EMERGENCY, 200, TimeUnit.MILLISECONDS);
                emergency = true;

            }

            //change motion
            double dx = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
            double dz = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
            double dy = mc.thePlayer.posY - mc.thePlayer.lastTickPosY;

            if (dx == 0 && dz == 0 && !notInIsland && !emergency ){
                if(Config.FarmType.equals(FarmEnum.VERTICAL)){

                    if(dy != 0 && !set) {
                        set = true;
                        ScheduleRunnable(changeMotion, 1, TimeUnit.SECONDS);
                    }

                } else {

                    if(!set3 && (mc.thePlayer.posZ != initialZ || mc.thePlayer.posX != initialX)) {

                            set3 = true;
                            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                            executor.execute(Motion3);
                            executor.shutdown();
                    }
                }


            }


            // Processes //
            if (process1 && !process3 && !process4) {


                if (shdBePressingKey) {

                    KeyBinding.setKeyBindState(keybindAttack, true);
                    KeyBinding.setKeyBindState(keybindS, false);
                    error = false;

                    KeyBinding.setKeyBindState(keybindD, true);
                    KeyBinding.setKeyBindState(keybindA, false);
                    KeyBinding.setKeyBindState(keybindW, false);
                    if(!setspawned){mc.thePlayer.sendChatMessage("/setspawn"); setspawned = true;}

                }

            } else if (process2 && !process3 && !process4) {


                setspawned = false;
                if (shdBePressingKey) {

                    KeyBinding.setKeyBindState(keybindAttack, true);
                    KeyBinding.setKeyBindState(keybindS, false);
                    KeyBinding.setKeyBindState(keybindA, true);
                    KeyBinding.setKeyBindState(keybindD, false);
                    KeyBinding.setKeyBindState(keybindW, false);

                }

            }
            if(process3 && !process4){
                if (shdBePressingKey)
                KeyBinding.setKeyBindState(keybindW, true);
            } else if(process4){
                process3 = false;
                if(shdBePressingKey) {

                    KeyBinding.setKeyBindState(keybindW, false);
                    KeyBinding.setKeyBindState(keybindS, true);

                }
            }

        } else{
            locked = false;
        }



    }


    //multi-threads

     Runnable checkChange = new Runnable() {
        @Override
        public void run() {

            if(!notInIsland && !emergency && enabled) {
                 deltaX = Math.abs(mc.thePlayer.posX - beforeX);
                 deltaZ = Math.abs(mc.thePlayer.posZ - beforeZ);

                 beforeX = mc.thePlayer.posX;
                 beforeZ = mc.thePlayer.posZ;
                ScheduleRunnable(checkChange, 3, TimeUnit.SECONDS);

            }

        }
    };


    Runnable changeMotion = new Runnable() {
        @Override
        public void run() {
            if(!notInIsland && !emergency) {
                process1 = !process1;
                process2 = !process2;
                set = false;
            }
        }
    };

    Runnable stopAntistuck = new Runnable() {
        @Override
        public void run() {
            deltaX = 10000;
            deltaZ = 10000;
            process4 = false;
            setAntiStuck = false;
        }
    };

    Runnable Motion3 = new Runnable() {
        @Override
        public void run() {

            if(!notInIsland && !emergency) {

                process3 = !process3;
                initialX = mc.thePlayer.posX;
                initialZ = mc.thePlayer.posZ;


                if(!process3){
                    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                    executor.execute(changeMotion);
                }
                set3 = false;

            }
        }
    };


    Runnable LeaveSBIsand = new Runnable() {
        @Override
        public void run() {
            mc.thePlayer.sendChatMessage("/l");
            ScheduleRunnable(Rejoin, 8, TimeUnit.SECONDS);
        }
    };

    Runnable Rejoin = new Runnable() {
        @Override
        public void run() {
            mc.thePlayer.sendChatMessage("/play sb");
            ScheduleRunnable(WarpHome, 8, TimeUnit.SECONDS);
        }
    };

    Runnable WarpHome = new Runnable() {
        @Override
        public void run() {
            mc.thePlayer.sendChatMessage("/warp home");
            ScheduleRunnable(afterRejoin1, 8, TimeUnit.SECONDS);
        }
    };



    Runnable afterRejoin1 = new Runnable() {
        @Override
        public void run() {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
            if(!error) {
                ScheduleRunnable(afterRejoin2, 2500, TimeUnit.MILLISECONDS);
            } else {
                ScheduleRunnable(WarpHome, 5, TimeUnit.SECONDS);
                error = false;
            }

        }
    };
    Runnable afterRejoin2 = new Runnable() {
        @Override
        public void run() {

            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);

            initialize();

            mc.inGameHasFocus = true;
            mouseHelper.grabMouseCursor();
            mc.displayGuiScreen((GuiScreen)null);
            Field f = null;
            f = FieldUtils.getDeclaredField(mc.getClass(), "leftClickCounter",true);
            try {
               f.set(mc, 10000);
            }catch (Exception e){
                e.printStackTrace();
            }

            ScheduleRunnable(checkChange, 3, TimeUnit.SECONDS);
        }
    };
    Runnable checkPriceChange = new Runnable() {
        @Override
        public void run() {

            if(!(prevMoney == -999) && (totalMoney - prevMoney >= 0)) {
                moneyper10sec = totalMoney - prevMoney;
            }

            prevMoney = totalMoney;

            ScheduleRunnable(checkPriceChange, 10, TimeUnit.SECONDS);
        }
    };

    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event){

        if(Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)){
            openedGUI = true;
            mc.displayGuiScreen(new GUI());
        }
        if(Keyboard.isKeyDown(Keyboard.KEY_GRAVE)){
            toggle();
        }


    }

    Runnable EMERGENCY = new Runnable() {
        @Override
        public void run() {

            KeyBinding.setKeyBindState(keybindAttack, false);
            KeyBinding.setKeyBindState(keybindA, false);
            KeyBinding.setKeyBindState(keybindW, false);
            KeyBinding.setKeyBindState(keybindD, false);
            KeyBinding.setKeyBindState(keybindS, false);


            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.schedule(SHUTDOWN, 4123, TimeUnit.MILLISECONDS);


        }
    };

    Runnable SHUTDOWN = new Runnable() {
        @Override
        public void run() {
            mc.shutdown();
        }
    };


    /*Runnable Reconnect = new Runnable() {
        @Override
        public void run() {
            FMLClientHandler.instance().connectToServer(new GuiMainMenu(), sd);
        }
    };*/
/*
    String[] uuid = new String[]
            {
                    "f47ae565-4843-450f-bb5e-08eea3524aa6",
                    "6cc977b4-135c-4032-8b98-db933e61efdf",
                    "7bbbc85c-56d3-47b3-9aac-c77751011377",
                    "b049e1c7-4f5a-4579-a90a-fb5cc1fea4c9",
                    "dd25b76f-a852-48d2-bbfd-2ad0b436b5f7",
                    "dc313486-1e47-4792-843b-863b44b8a1bc",
                    "b541f53d-99c8-401b-a364-b0f10cc24d8b",
                    "3ec28f98-4758-43ce-8218-765c6b4d17c9",
                    "b06db246-c073-4ce6-9398-8b500309ae3a",
                    "26a011c6-ba0e-42be-8ca7-1b726420fa22",
                    "f2ded78e-32de-4d09-87d6-4a8eae1ab890",
                    "80859660-3452-40af-96d6-f82c1bc72bd2",
                    "21613d3c-859f-4c4f-bd6c-9b258a4d481d",
                    "83314de0-8724-47d4-b849-c36975cfa4d0",
                    "5cfaf73b-0dc4-4f16-b4b9-513ce462a39c"
            };*/


    //List<String> uuidList = new ArrayList<String>(Arrays.asList(uuid));

     void toggle(){
        mc.thePlayer.closeScreen();
        if(enabled){
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN +
                    "[Farm Helper] : " + EnumChatFormatting.DARK_GREEN + "Stopped script"));
            stop();
        }
        enabled = !enabled;
        openedGUI = false;
    }
    void stop(){
        net.minecraft.client.settings.KeyBinding.setKeyBindState(keybindA, false);
        net.minecraft.client.settings.KeyBinding.setKeyBindState(keybindW, false);
        net.minecraft.client.settings.KeyBinding.setKeyBindState(keybindD, false);
        net.minecraft.client.settings.KeyBinding.setKeyBindState(keybindS, false);
        net.minecraft.client.settings.KeyBinding.setKeyBindState(keybindAttack, false);
    }
    void activateFailsafe(){
         shdBePressingKey = false;
         notInIsland = true;
         KeyBinding.setKeyBindState(keybindAttack, false);
        process1 = false;
        process2 = false;
        process3 = false;
        process4 = false;

    }
    Block playerBlock(){
         return mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)).getBlock();
    }

    void ScheduleRunnable(Runnable r, int delay, TimeUnit tu){
        ScheduledExecutorService eTemp = Executors.newScheduledThreadPool(1);
        eTemp.schedule(r, delay, tu);
        eTemp.shutdown();
    }
    void initialize(){
        deltaX = 10000;
        deltaZ = 10000;

        process1 = true;
        process2 = false;
        process3 = false;
        process4 = false;

        shdBePressingKey = true;
        notInIsland = false;
        beforeX = mc.thePlayer.posX;
        beforeZ = mc.thePlayer.posZ;
        initialX = mc.thePlayer.posX;
        initialZ = mc.thePlayer.posZ;
        set = false;
        set3 = false;
    }
    int angleToValue(AngleEnum c){
         return !c.toString().replace("A", "").contains("N") ?
                 Integer.parseInt(c.toString().replace("A", "")) :
                 Integer.parseInt(c.toString().replace("A", "").replace("N", "")) * -1;
    }


}
