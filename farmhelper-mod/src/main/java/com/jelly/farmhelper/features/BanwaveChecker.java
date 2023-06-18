package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.network.APIHelper;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.LinkedList;

public class BanwaveChecker {
    private final Minecraft mc = Minecraft.getMinecraft();
    public static String staffBan = "Staff ban: NaN";


    private static final Clock cooldown = new Clock();
    private static final LinkedList<Integer> staffBanLast15Mins = new LinkedList<>();
    public volatile static boolean banwaveOn = false;
    public static final Clock leaveTime = new Clock();
    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END)
            return;
        if((!cooldown.isScheduled() || cooldown.passed()) && FarmHelper.config.banwaveCheckerEnabled){
            new Thread(() -> {
                try {
                    String s = APIHelper.readJsonFromUrl("https://api.plancke.io/hypixel/v1/punishmentStats", "User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36")
                            .get("record").toString();
                    JSONParser parser = new JSONParser();
                    JSONObject record = (JSONObject) parser.parse(s);

                    staffBanLast15Mins.addLast((Integer.parseInt(record.get("staff_total").toString())));
                    if(staffBanLast15Mins.size() == 17) staffBanLast15Mins.removeFirst();

                    staffBan = getBanDisplay();

                    if(banwaveOn) {
                        if (getBanTimeDiff() != 0)
                            banwaveOn = getBanDiff() / (getBanTimeDiff() * 1.0f) > (FarmHelper.config.banwaveThreshold * 0.8f)/ 15.0f;
                    } else {
                        if (getBanTimeDiff() != 0)
                            banwaveOn = getBanDiff() / (getBanTimeDiff() * 1.0f) > FarmHelper.config.banwaveThreshold / 15.0f;
                    }

                    if(MacroHandler.isMacroing && FarmHelper.config.enableLeaveOnBanwave) {
                        if (banwaveOn && mc.theWorld != null && !Failsafe.emergency) {
                            LogUtils.webhookLog("Disconnecting due to banwave detected");

                            MacroHandler.disableCurrentMacro();
                            if (FarmHelper.config.setSpawnBeforeEvacuate)
                                PlayerUtils.setSpawn();
                            if (leaveTime.isScheduled() && leaveTime.passed()) {
                                leaveTime.reset();
                                this.mc.theWorld.sendQuittingDisconnectingPacket();
                            } else if (!leaveTime.isScheduled()) {
                                leaveTime.schedule(3_000);
                            }

                        }
                    }

                } catch(Exception e){
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }).start();
            cooldown.schedule(60000);
        }
    }
    private static int getBanTimeDiff(){
        return staffBanLast15Mins.size() > 1 ? staffBanLast15Mins.size() - 1 : 0;
    }
    public static int getBanDiff(){
        return staffBanLast15Mins.size() > 1 ? Math.abs(staffBanLast15Mins.getLast() - staffBanLast15Mins.getFirst()) : 0;
    }
    public static String getBanDisplay(){
        return getBanTimeDiff() > 0 ? "Staff ban in last " + getBanTimeDiff() + " minutes: " + getBanDiff() : "Staff ban: Collecting data...";
    }

}
