package com.jelly.farmhelperv2.feature.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.util.LogUtils;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PetEquipListener implements IFeature {

    private static PetEquipListener instance;
    private static final Pattern SUMMON_PET = Pattern.compile("^You summoned your (.+?)(?: Pet)?!$");

    private static boolean hasEquippedPet = false;

    private boolean running = false;

    private PetEquipListener() { }

    public static PetEquipListener getInstance() {
        if (instance == null) {
            instance = new PetEquipListener();
        }
        return instance;
    }

    public static boolean hasEquippedPet() {
        return hasEquippedPet;
    }

    @Override public String getName() { return "Pet Equip Listener"; }
    @Override public boolean isRunning() { return running; }
    @Override public boolean isToggled() { return true; }
    @Override public boolean shouldPauseMacroExecution() { return false; }
    @Override public boolean shouldStartAtMacroStart() { return !hasEquippedPet; }
    @Override public boolean shouldCheckForFailsafes() { return true; }
    @Override public void resetStatesAfterMacroDisabled() { stop(); }

    @Override
    public void start() {
        if (running || hasEquippedPet) return;

        running = true;
        LogUtils.sendDebug("[PetEquip] listener started");
        IFeature.super.start();
    }

    @Override
    public void stop() {
        if (!running || !hasEquippedPet) return;

        running = false;
        LogUtils.sendDebug("[PetEquip] listener stopped");
        IFeature.super.stop();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!running || event.type != 0) return;

        String msg = StringUtils.stripControlCodes(event.message.getUnformattedText());

        Matcher m = SUMMON_PET.matcher(msg);
        if (m.matches()) {
            String petName = m.group(1);
            LogUtils.sendDebug("[PetEquip] you just summoned: " + petName);
            for (VerifyFarmingEquipment.FarmingPet fp : VerifyFarmingEquipment.FarmingPet.values()) {
                if (petName.contains(fp.name())) {
                    hasEquippedPet = true;
                    stop();
                }
            }
        }
    }
}
