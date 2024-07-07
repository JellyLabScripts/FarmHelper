package com.jelly.farmhelperv2.util.helper;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.util.LogUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.sound.sampled.*;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class AudioManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static AudioManager instance;

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    @Getter
    @Setter
    private boolean minecraftSoundEnabled = false;

    private final Clock delayBetweenPings = new Clock();
    private int numSounds = 15;
    @Setter
    private float soundBeforeChange = 0;

    public void resetSound() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
            return;
        }
        minecraftSoundEnabled = false;
        if (FarmHelperConfig.maxOutMinecraftSounds) {
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, soundBeforeChange);
        }
    }

    private static Clip clip;

    public void playSound() {
        if (!FarmHelperConfig.failsafeSoundType) {
            if (minecraftSoundEnabled) return;
            numSounds = 15;
            minecraftSoundEnabled = true;
            if (FarmHelperConfig.maxOutMinecraftSounds) {
                mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 1.0f);
            }
        } else {
            Multithreading.schedule(() -> {
                try {
                    AudioInputStream inputStream = null;
                    switch (FarmHelperConfig.failsafeSoundSelected) {
                        case 0:
                            File audioFile = new File(mc.mcDataDir.getAbsolutePath() + "/farmhelper_sound.wav");
                            if (audioFile.exists() && audioFile.isFile())
                                inputStream = AudioSystem.getAudioInputStream(audioFile);
                            break;
                        case 1:
                            inputStream = AudioSystem.getAudioInputStream(getClass().getResource("/farmhelper/sounds/staff_check_voice_notification.wav"));
                            break;
                        case 2:
                            inputStream = AudioSystem.getAudioInputStream(getClass().getResource("/farmhelper/sounds/metal_pipe.wav"));
                            break;
                        case 3:
                            inputStream = AudioSystem.getAudioInputStream(getClass().getResource("/farmhelper/sounds/AAAAAAAAAA.wav"));
                            break;
                        case 4:
                            inputStream = AudioSystem.getAudioInputStream(getClass().getResource("/farmhelper/sounds/loud_buzz.wav"));
                            break;
                    }
                    if (inputStream == null) {
                        LogUtils.sendError("[Audio Manager] Failed to load sound file!");
                        return;
                    }
                    clip = AudioSystem.getClip();
                    clip.open(inputStream);
                    FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float volumePercentage = FarmHelperConfig.failsafeSoundVolume / 100f;
                    float dB = (float) (Math.log(volumePercentage) / Math.log(10.0) * 20.0);
                    volume.setValue(dB);
                    clip.start();
                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            clip.close();
                        }
                    });
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }, 0, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isSoundPlaying() {
        return (clip != null && clip.isRunning()) || minecraftSoundEnabled;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (FarmHelperConfig.failsafeSoundType) return;
        if (!minecraftSoundEnabled) return;
        if (delayBetweenPings.isScheduled() && !delayBetweenPings.passed()) return;
        if (numSounds <= 0) {
            minecraftSoundEnabled = false;
            if (FarmHelperConfig.maxOutMinecraftSounds) {
                mc.gameSettings.setSoundLevel(SoundCategory.MASTER, soundBeforeChange);
            }
            return;
        }

        switch (FarmHelperConfig.failsafeMcSoundSelected) {
            case 0: {
                mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.orb", 10.0F, 1.0F, false);
                break;
            }
            case 1: {
                mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.anvil_land", 10.0F, 1.0F, false);
                break;
            }
        }
        delayBetweenPings.schedule(100);
        numSounds--;
    }
}
