package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

// I can explain
// https://imgur.com/a/IXFrfYz
// thanks <3
public class RussianRoulette implements IFeature {
    private boolean startDuping = false;
    public static RussianRoulette instance = null;
    public static RussianRoulette getInstance(){
        if(instance == null){
            instance = new RussianRoulette();
        }
        return instance;
    }
    @Override
    public String getName() {
        return "RussianRoulette";
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    @Override
    public void start() {
        // haha get duked it runs always
    }

    @Override
    public void stop() {
        // haha once it starts you dont stop it
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        // no restart ahhahaha
    }

    @Override
    public boolean isToggled() {
        return true;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
        // what failsafe its a ban machine
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event){
        startDuping = new Random().nextInt(100_000_000) == new Random().nextInt(100_000_000);
    }

    @SubscribeEvent
    public void startDupe(RenderWorldLastEvent event){
        if(!startDuping) return;
        KeyBindUtils.leftClick();
    }
}
