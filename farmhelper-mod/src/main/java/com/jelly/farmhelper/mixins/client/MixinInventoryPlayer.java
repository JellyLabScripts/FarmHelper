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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
                if (postAddInventory[i] != null) {
                    int size = 0;

                    if (preAddInventory[i] == null) {
                        size = postAddInventory[i].stackSize;
                    } else if (preAddInventory[i].getItem() == postAddInventory[i].getItem()) {
                        if (preAddInventory[i].stackSize < postAddInventory[i].stackSize) {
                            size = postAddInventory[i].stackSize - preAddInventory[i].stackSize;
                        } else if (postAddInventory[i].stackSize < preAddInventory[i].stackSize) {
                            size = preAddInventory[i].stackSize - postAddInventory[i].stackSize;
                        }
                    } else if (preAddInventory[i].getItem() != postAddInventory[i].getItem()) {
                        size = postAddInventory[i].stackSize;
                    }


//                    if (preAddInventory[i] != null && preAddInventory[i].getItem() == postAddInventory[i].getItem() && preAddInventory[i].stackSize != postAddInventory[i].stackSize) {
//                        if (preAddInventory[i].stackSize < postAddInventory[i].stackSize)
//                            size = postAddInventory[i].stackSize - preAddInventory[i].stackSize;
//                        else
//                            size = preAddInventory[i].stackSize - postAddInventory[i].stackSize;
//                    } else if ((preAddInventory[i] == null || (preAddInventory[i].getItem() != postAddInventory[i].getItem())) && postAddInventory[i].stackSize >= 0) {
//                        size = postAddInventory[i].stackSize;
//                    }
                    if (size > 0)
                        checkForCompactAmount(i, size, postAddInventory);
                }
            }
        }
    }

    private void checkForCompactAmount(int i, int size, ItemStack[] postAddInventory) {
        if (StringUtils.stripControlCodes(postAddInventory[i].getDisplayName()).equals("Hay Bale")) {
            if (getAmountOfItemInInventory(postAddInventory[i].getDisplayName(), preAddInventory) + size >= 144) {
                int newSize = 144 - getAmountOfItemInInventory(postAddInventory[i].getDisplayName(), preAddInventory);
                if (newSize > 0)
                    size -= newSize;
            }
        } else if (!StringUtils.stripControlCodes(postAddInventory[i].getDisplayName()).equals("Cropie") &&
                !StringUtils.stripControlCodes(postAddInventory[i].getDisplayName()).equals("Squash") &&
                !StringUtils.stripControlCodes(postAddInventory[i].getDisplayName()).equals("Fermento") &&
                !StringUtils.stripControlCodes(postAddInventory[i].getDisplayName()).equals("Burrowing Spores")) {
            if (getAmountOfItemInInventory(postAddInventory[i].getDisplayName(), preAddInventory) + size >= 160) {
                int newSize = 160 - getAmountOfItemInInventory(postAddInventory[i].getDisplayName(), preAddInventory);
                if (newSize > 0)
                    size -= newSize;
            }
        }
        if (size > 0)
            ProfitCalculator.onInventoryChanged(postAddInventory[i], size);
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
