package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.AutoSellConfig;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.network.APIHelper;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.InventoryUtils;
import com.jelly.farmhelper.utils.KeyBindUtils;
import com.jelly.farmhelper.utils.LogUtils;
import com.typesafe.config.ConfigException;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BanwaveChecker {
   /* private static final Minecraft mc = Minecraft.getMinecraft();
    private static Clock cooldown = new Clock();
    private static int lastban = -1;
    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || !MacroHandler.isMacroOn)
            return;
        if(!cooldown.isScheduled() || cooldown.passed()){
            try {
         //       lastban = Integer.parseInt();
                LogUtils.scriptLog(APIHelper.readJsonFromUrl("https://api.plancke.io/hypixel/v1/punishmentStats", "User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36")
                        .get("record").toString());

            } catch(Exception e){
                LogUtils.scriptLog(e.getMessage());
            }
            cooldown.schedule(5000);
        }

    }*/
}
