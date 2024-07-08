package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.UpdateTablistEvent;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.GameStateHandler.BuffState;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class AutoSprayonator implements IFeature {

  private static AutoSprayonator instance;

  public static AutoSprayonator getInstance() {
    if (instance == null) {
      instance = new AutoSprayonator();
    }
    return instance;
  }

  private final String[] SPRAY_MATERIAL = {"Fine Flour", "Compost", "Honey Jar", "Dung", "Plant Matter", "Tasty Cheese"};
  private final Minecraft mc = Minecraft.getMinecraft();
  private boolean enabled = false;
  private boolean pause = false;
  private State state = State.STARTING;
  private final Clock timer = new Clock();

  @Override
  public String getName() {
    return "AutoSprayonator";
  }

  @Override
  public boolean isRunning() {
    return this.enabled;
  }

  @Override
  public boolean shouldPauseMacroExecution() {
    return true;
  }

  @Override
  public boolean shouldStartAtMacroStart() {
    return false;
  }

  @Override
  public void resetStatesAfterMacroDisabled() {
    this.pause = false;
    this.state = State.STARTING;
  }

  @Override
  public boolean isToggled() {
    return FarmHelperConfig.autoSprayonator;
  }

  @Override
  public boolean shouldCheckForFailsafes() {
    return false;
  }

  private String getSprayMaterial() {
    return this.SPRAY_MATERIAL[FarmHelperConfig.autoSprayonatorSprayMaterial];
  }

  @Override
  public void start() {
    if (this.enabled) {
      return;
    }

    if (!InventoryUtils.hasItemInHotbar("Sprayonator")) {
      LogUtils.sendError("Cannot find sprayonator in hotbar. Pausing until restart.");
      this.pause = true;
      return;
    }

    boolean correctMaterialSelected = false;

    for (String lore : InventoryUtils.getLoreOfItemInContainer(InventoryUtils.getSlotIdOfItemInContainer("Sprayonator"))) {
      if (lore.startsWith("Selected Material")) {
        correctMaterialSelected = lore.endsWith(this.getSprayMaterial());
        break;
      }
    }

    if (!correctMaterialSelected) {
      LogUtils.sendError("Please select " + this.getSprayMaterial() + " as your spray material. Pausing until restart.");
      this.pause = true;
      return;
    }

    MacroHandler.getInstance().pauseMacro();
    this.timer.schedule(FarmHelperConfig.autoSprayonatorAdditionalDelay);
    this.enabled = true;
  }

  @Override
  public void stop() {
    this.enabled = false;
    this.state = State.STARTING;
    if (MacroHandler.getInstance().isMacroToggled()) {
      MacroHandler.getInstance().resumeMacro();
    }
  }

  @SubscribeEvent
  public void onTablistUpdate(UpdateTablistEvent event) {
    if (!this.isToggled() || !MacroHandler.getInstance().isCurrentMacroEnabled() || this.enabled || this.pause) {
      return;
    }
    if (this.isTimerRunning()) {
      return;
    }
    if (!GameStateHandler.getInstance().inGarden()) {
      return;
    }
    if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) {
      return;
    }
    if (!Scheduler.getInstance().isFarming()) {
      return;
    }
    if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) {
      return;
    }
    if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) {
      return;
    }
    if (GameStateHandler.getInstance().getSprayonatorState() != BuffState.NOT_ACTIVE) {
      return;
    }

    this.start();
  }

  @SubscribeEvent
  public void onChatEvent(ClientChatReceivedEvent event) {
    if (!this.enabled || this.state != State.WAITING) {
      return;
    }

    final String message = event.message.getUnformattedText();
    if (message.startsWith("SPRAYONATOR!") || message.equals("This plot was sprayed with that item recently! Try again soon!")) {
      this.swapState(State.END, FarmHelperConfig.autoSprayonatorAdditionalDelay);
      return;
    }

    if (message.startsWith("You don't have any ")) {
      if (!FarmHelperConfig.autoSprayonatorAutoBuyItem) {
        LogUtils.sendError("Don't have any spray item and not allowed to buy any. Pausing until restart.");
        this.pause = true;
        this.swapState(State.END, FarmHelperConfig.autoSprayonatorAdditionalDelay);
      } else {
        this.swapState(State.BUYING_MATERIAL, FarmHelperConfig.autoSprayonatorAdditionalDelay);
      }
    }
  }

  @SubscribeEvent
  public void onTickSpray(ClientTickEvent event) {
    if (!this.enabled) {
      return;
    }

    switch (this.state) {
      case STARTING:
        if (this.isTimerRunning()) {
          break;
        }
        if (!InventoryUtils.holdItem("Sprayonator")) {
          LogUtils.sendError("Cannot hold sprayonator. Pausing until restart");
          this.pause = true;
          this.stop();
          break;
        }
        this.swapState(State.SPRAYING, FarmHelperConfig.autoSprayonatorAdditionalDelay);
        break;
      case SPRAYING:
        if (this.isTimerRunning()) {
          break;
        }
        KeyBindUtils.rightClick();
        this.swapState(State.WAITING, 5000);
        break;
      case BUYING_MATERIAL:
        if (this.isTimerRunning()) {
          break;
        }
        AutoBazaar.getInstance().buy(this.getSprayMaterial(), FarmHelperConfig.autoSprayonatorAutoBuyAmount);
        this.swapState(State.WAITING_FOR_PURCHASE, 20000);
        break;
      case WAITING_FOR_PURCHASE:
        if (!this.isTimerRunning()) {
          LogUtils.sendError("AutoBazaar took more than 20 seconds. Pausing until restart"); // should never toggle
          this.pause = true;
          this.stop();
          break;
        }

        if (AutoBazaar.getInstance().isRunning()) {
          break;
        }

        if (AutoBazaar.getInstance().hasFailed()) {
          LogUtils.sendError("AutoBazaar could not buy " + this.getSprayMaterial() + ". Pausing until restart");
          this.pause = true;
          this.stop();
          break;
        }

        this.swapState(State.STARTING, FarmHelperConfig.autoSprayonatorAdditionalDelay);
        break;
      case WAITING:
        if (!this.isTimerRunning()) {
          LogUtils.sendError("Could not verify spray before time ended.");
          this.stop();
        }
        break;
      case END:
        if (this.isTimerRunning()) {
          break;
        }
        this.timer.schedule(2000);
        this.stop();
        break;
    }
  }

  public void swapState(State swapTo, int delay) {
    this.state = swapTo;
    this.timer.schedule(delay);
  }

  public boolean isTimerRunning() {
    return this.timer.isScheduled() && !this.timer.passed();
  }

  enum State {
    STARTING, SPRAYING, BUYING_MATERIAL, WAITING_FOR_PURCHASE, WAITING, END
  }
}
