package com.jelly.farmhelperv2.util;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TablistUtils {

    public static final Ordering<NetworkPlayerInfo> playerOrdering = Ordering.from(new PlayerComparator());

    private static final CopyOnWriteArrayList<String> cachedTablist = new CopyOnWriteArrayList<>();

    public static List<String> getTabList() {
        return cachedTablist;
    }

    public static void setCachedTablist(List<String> tablist) {
        cachedTablist.clear();
        cachedTablist.addAll(tablist);
    }

    @SideOnly(Side.CLIENT)
    static class PlayerComparator implements Comparator<NetworkPlayerInfo> {
        private PlayerComparator() {
        }

        public int compare(NetworkPlayerInfo o1, NetworkPlayerInfo o2) {
            ScorePlayerTeam team1 = o1.getPlayerTeam();
            ScorePlayerTeam team2 = o2.getPlayerTeam();
            return ComparisonChain.start().compareTrueFirst(
                            o1.getGameType() != WorldSettings.GameType.SPECTATOR,
                            o2.getGameType() != WorldSettings.GameType.SPECTATOR
                    )
                    .compare(
                            team1 != null ? team1.getRegisteredName() : "",
                            team2 != null ? team2.getRegisteredName() : ""
                    )
                    .compare(o1.getGameProfile().getName(), o2.getGameProfile().getName()).result();
        }
    }
}
