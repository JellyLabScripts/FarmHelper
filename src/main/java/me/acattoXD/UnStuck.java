package me.acattoXD;

import com.jelly.FarmHelper.utils.LogUtils;
import net.minecraft.client.Minecraft;
import java.util.concurrent.TimeUnit;
import static me.acattoXD.WartMacro.lastStuck;

public class UnStuck {
    public static int stuckCount;

    public static boolean teleporting;
    public static boolean stuck;
    public static long stuckCooldown;

    private static final Minecraft mc = Minecraft.getMinecraft();


    public static void stuckFrequency() {
        if (System.currentTimeMillis() - lastStuck < 30000) {
            stuckCount++;
        } else {
            stuckCount = 1;
        }
        if (stuckCount >= 3) {
            LogUtils.debugLog("Stuck 3 times in succession - Going to lobby");
            LogUtils.webhookLog("Stuck 3 times in succession - Going to lobby");
            stuckCount = 1;
            teleporting = false;
            stuck = false;
            setStuckCooldown(5);
            mc.thePlayer.sendChatMessage("/lobby");
        }
        lastStuck = System.currentTimeMillis();
    }

    public static void setStuckCooldown(int seconds) {
        stuckCooldown = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
    }
}
