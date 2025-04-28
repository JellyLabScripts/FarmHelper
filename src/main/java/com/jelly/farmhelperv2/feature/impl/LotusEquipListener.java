package com.jelly.farmhelperv2.feature.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.util.LogUtils;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LotusEquipListener implements IFeature {

    private static LotusEquipListener instance;
    private static final Pattern EQUIP_LOTUS = Pattern.compile("^You equipped a (.+ Lotus .+)!$");

    private static boolean hasEquippedLotusItem = false;

    private boolean running = false;

    private LotusEquipListener() { }

    public static LotusEquipListener getInstance() {
        if (instance == null) {
            instance = new LotusEquipListener();
        }
        return instance;
    }

    public static boolean hasEquippedLotus() { return hasEquippedLotusItem; }

    @Override public String getName() { return "Lotus Equip Listener"; }
    @Override public boolean isRunning() { return running; }
    @Override public boolean isToggled() { return true; }
    @Override public boolean shouldPauseMacroExecution() { return false; }
    @Override public boolean shouldStartAtMacroStart() { return !hasEquippedLotusItem; }
    @Override public boolean shouldCheckForFailsafes() { return true; }
    @Override public void resetStatesAfterMacroDisabled() { stop(); }

    @Override
    public void start() {
        if (running || hasEquippedLotusItem) return;

        running = true;
        LogUtils.sendDebug("[LotusEquip] listener started");
        IFeature.super.start();
    }

    @Override
    public void stop() {
        if (!running || !hasEquippedLotusItem) return;

        running = false;
        LogUtils.sendDebug("[LotusEquip] listener stopped");
        IFeature.super.stop();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!running || event.type != 0) return;

        String msg = StringUtils.stripControlCodes(
                event.message.getUnformattedText()
        );

        Matcher m = EQUIP_LOTUS.matcher(msg);
        if (m.matches()) {
            String fullItem = m.group(1);
            LogUtils.sendDebug("[LotusEquip] you just equipped: " + fullItem);

            // mark it, then shut ourselves down immediately
            hasEquippedLotusItem = true;
            stop();
        }
    }
}
