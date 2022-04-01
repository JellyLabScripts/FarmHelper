package FarmHelper;

import FarmHelper.GUI.GUI;
import FarmHelper.Utils.Utils;
import FarmHelper.config.AngleEnum;
import FarmHelper.config.Config;
import FarmHelper.config.CropEnum;
import FarmHelper.config.FarmEnum;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
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

    /*
    ** @author JellyLab
     */
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
    boolean rotating = false;


    double beforeX = 0;
    double beforeZ = 0;
    double deltaX = 10000;
    double deltaZ = 10000;
    double initialX = 0;
    double initialZ = 0;
    double initialY = 0;



    boolean notInIsland = false;
    boolean shdBePressingKey = true;
    public static boolean openedGUI = false;

    public int keybindA = mc.gameSettings.keyBindLeft.getKeyCode();
    public int keybindD = mc.gameSettings.keyBindRight.getKeyCode();
    public int keybindW = mc.gameSettings.keyBindForward.getKeyCode();
    public int keybindS = mc.gameSettings.keyBindBack.getKeyCode();
    public int keybindAttack = mc.gameSettings.keyBindAttack.getKeyCode();

    static KeyBinding[] customKeyBinds = new KeyBinding[2];

    static volatile int totalMnw = 0;
    static volatile int totalEnw = 0;
    static volatile int totalMoney = 0;
    static volatile int prevMoney = -999;
    int cycles = 0;
    static volatile int moneyper10sec = 0;

    MouseHelper mouseHelper = new MouseHelper();
    int playerYaw = 0;




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
        customKeyBinds[0] = new KeyBinding("Open GUI", Keyboard.KEY_RSHIFT, "FarmHelper");
        customKeyBinds[1] = new KeyBinding("Toggle script", Keyboard.KEY_GRAVE, "FarmHelper");
        ClientRegistry.registerKeyBinding(customKeyBinds[0]);
        ClientRegistry.registerKeyBinding(customKeyBinds[1]);
    }


    @SubscribeEvent
    public void onOpenGui(final GuiOpenEvent event) {
        if (event.gui instanceof GuiDisconnected) {
            enabled = false;
        }
    }


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
            ExecuteRunnable(changeLayer);
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
    public void OnTickPlayer(TickEvent.ClientTickEvent event) { //Client -> player

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
            //angles (locked)
            if(!emergency && !notInIsland) {
                if (Config.CropType.equals(CropEnum.NETHERWART)) {
                    mc.thePlayer.rotationPitch = 0;

                } else {
                    mc.thePlayer.rotationPitch = 6;
                }
                mc.thePlayer.rotationYaw = playerYaw;

            }
            //INITIALIZE
            if (!locked) {
                KeyBinding.setKeyBindState(keybindA, false);
                KeyBinding.setKeyBindState(keybindD, false);
                locked = true;
                initialize();
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

            }else if(deltaX < 0.1d && deltaZ < 0.1d && !notInIsland && !emergency && !setAntiStuck && Config.FarmType.equals(FarmEnum.VERTICAL) && initialY != mc.thePlayer.posY){
                 //tp pad fix
                initialY = mc.thePlayer.posY;
                deltaX = 10000;
                deltaZ = 10000;
                ScheduleRunnable(changeMotion, 300, TimeUnit.MILLISECONDS);
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
            Block blockIn = mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)).getBlock();

            double dx = Math.abs(mc.thePlayer.posX - mc.thePlayer.lastTickPosX);
            double dz = Math.abs(mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ);
            double dy = Math.abs(mc.thePlayer.posY - mc.thePlayer.lastTickPosY);
            boolean falling = blockIn == Blocks.air && dy != 0;
            if ((float)dx == 0 && dz == 0 && !notInIsland && !emergency){// changed == 0 to < 0.1d
                if(Config.FarmType.equals(FarmEnum.VERTICAL)){
                    if(dy != 0 && !set) {
                        set = true;
                        ScheduleRunnable(changeMotion, 1, TimeUnit.SECONDS);
                    }
                } else {
                    if(falling && !rotating){
                        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN +
                                "[Farm Helper] : " + EnumChatFormatting.DARK_GREEN + "New layer detected"));
                        ExecuteRunnable(changeLayer);
                        enabled = false;

                    }
                    else if(!set3 && (mc.thePlayer.posZ != initialZ || mc.thePlayer.posX != initialX) && !rotating) {
                            set3 = true;
                            ExecuteRunnable(Motion3);
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
                    if(!setspawned){mc.thePlayer.sendChatMessage("/setspawn"); setspawned = true; cycles++;}
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
            if(cycles == 4 && Config.resync)
                ExecuteRunnable(reSync);

        } else{
            locked = false;
        }



    }


    //multi-threads

    Runnable reSync = new Runnable() {
        @Override
        public void run() {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN +
                    "[Farm Helper] : " + EnumChatFormatting.DARK_GREEN + "Resyncing.. disabling script (10s)"));
            enabled = false;
            stop();
            try{
                Thread.sleep(10000);
            }catch(Exception e){
              e.printStackTrace();
            }
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN +
                    "[Farm Helper] : " + EnumChatFormatting.DARK_GREEN + "Resyncing.. enabling script now"));
            enabled = true;
        }
    };
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

    Runnable changeLayer = new Runnable() {
        @Override
        public void run() {
            if(!notInIsland && !emergency) {
                try {
                    stop();
                    rotating = true;
                    enabled = false;
                    Thread.sleep(500);
                    if(mc.thePlayer.rotationYaw != angleToValue(Config.Angle))
                       mc.thePlayer.rotationYaw = mc.thePlayer.rotationYaw + 180;
                    else
                        mc.thePlayer.rotationYaw = mc.thePlayer.rotationYaw - 180;
                    Thread.sleep(500);
                    rotating = false;
                    playerYaw = (int)mc.thePlayer.rotationYaw;
                    enabled = true;
                }catch(Exception e){
                    e.printStackTrace();
                }
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

        if(!rotating) {
            if (customKeyBinds[0].isPressed()) {
                openedGUI = true;
                mc.displayGuiScreen(new GUI());
            }
            if (customKeyBinds[1].isPressed()) {
                if (!enabled)
                    mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN +
                            "[Farm Helper] : " + EnumChatFormatting.DARK_GREEN + "Starting script"));

                toggle();
            }
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

           // mc.thePlayer.addChatMessage(ScreenShotHelper.saveScreenshot(mc.mcDataDir, mc.displayWidth, mc.displayHeight, mc.getFramebuffer()));

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

     void toggle(){
        mc.thePlayer.closeScreen();
        if(enabled){
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN +
                    "[Farm Helper] : " + EnumChatFormatting.DARK_GREEN + "Stopped script"));
            stop();
        } else {
            playerYaw = angleToValue(Config.Angle);
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
    void ExecuteRunnable(Runnable r){
        ScheduledExecutorService eTemp = Executors.newScheduledThreadPool(1);
        eTemp.execute(r);
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
        cycles = 0;
        rotating = false;
    }
    int angleToValue(AngleEnum c){
         return !c.toString().replace("A", "").contains("N") ?
                 Integer.parseInt(c.toString().replace("A", "")) :
                 Integer.parseInt(c.toString().replace("A", "").replace("N", "")) * -1;
    }
}
