package com.jelly.farmhelper.mixins.client;

import com.jelly.farmhelper.features.ProfitCalculator;
import com.jelly.farmhelper.utils.PlayerUtils;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InventoryPlayer.class})
public abstract class MixinInventoryPlayer {

    @Shadow
    public ItemStack[] mainInventory;

    private ItemStack[] preAddInventory = new ItemStack[36];
    private ItemStack[] postAddInventory = new ItemStack[36];

    @Inject(method = "setInventorySlotContents", at = @At("HEAD"))
    public void onInventoryChange(int index, ItemStack stack, CallbackInfo ci) {
        if (stack != null && stack.getItem() != null) {
            preAddInventory = PlayerUtils.copyInventory(mainInventory).toArray(new ItemStack[0]);
        }
    }

    @Inject(method = "setInventorySlotContents", at = @At("RETURN"))
    public void onInventoryChangeReturn(int index, ItemStack stack, CallbackInfo ci) {
        if (stack != null && stack.getItem() != null) {
            postAddInventory = PlayerUtils.copyInventory(mainInventory).toArray(new ItemStack[0]);
            for (int i = 0; i < 36; i++) {
                if (preAddInventory[i] != null && postAddInventory[i] != null) {
                    if ((preAddInventory[i].getItem() == postAddInventory[i].getItem() && preAddInventory[i].stackSize != postAddInventory[i].stackSize)
                        || (preAddInventory[i].getItem() != postAddInventory[i].getItem() && postAddInventory[i].stackSize >= 0)) {

                        int size = postAddInventory[i].stackSize - preAddInventory[i].stackSize;
                        if (StringUtils.stripControlCodes(preAddInventory[i].getDisplayName()).equals("Wheat")) {
                            if (getAmountOfItemInInventory(preAddInventory[i].getDisplayName(), preAddInventory) + size >= 144)
                                size -= 144 - getAmountOfItemInInventory(preAddInventory[i].getDisplayName(), preAddInventory);
                        } else {
                            if (getAmountOfItemInInventory(preAddInventory[i].getDisplayName(), preAddInventory) + size >= 160)
                                size -= 160 - getAmountOfItemInInventory(preAddInventory[i].getDisplayName(), preAddInventory);
                        }
                        if (size > 0)
                            ProfitCalculator.onInventoryChanged(postAddInventory[i], size);
                    }
                }
            }
        }
    }

    private int getAmountOfItemInInventory(String name, ItemStack[] inventory) {
        int amount = 0;
        for (ItemStack item : inventory) {
            if (item != null && item.getItem() != null) {
                if (item.getDisplayName().equals(name)) {
                    amount += item.stackSize;
                }
            }
        }
        return amount;
    }
}
