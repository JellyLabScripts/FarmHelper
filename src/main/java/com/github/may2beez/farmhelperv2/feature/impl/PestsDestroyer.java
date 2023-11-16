package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.util.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Tuple;
import net.minecraft.util.Vec3;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PestsDestroyer implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();

    private final List<Tuple<String, String>> pests = Arrays.asList(
            new Tuple<>("Beetle", "ewogICJ0aW1lc3RhbXAiIDogMTY5Njg3MDQzNTQwNCwKICAicHJvZmlsZUlkIiA6ICJiMDU4MTFjYTdmNDk0YTM5OTZiNDU4ZjcwMmQ2MzJiOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJVeWlsIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzM1NTkwZDUzMjZhNjVkNTViMmJjNjBjNWNkMTk0YzEzZDYxMjU2NThkM2Q0YzYwZWNlMWQ5YmVjZmFjZWE5M2MiCiAgICB9CiAgfQp9"),
            new Tuple<>("Cricklet", "ewogICJ0aW1lc3RhbXAiIDogMTY5Njg3MDQ1NDYxMywKICAicHJvZmlsZUlkIiA6ICI5MThhMDI5NTU5ZGQ0Y2U2YjE2ZjdhNWQ1M2VmYjQxMiIsCiAgInByb2ZpbGVOYW1lIiA6ICJCZWV2ZWxvcGVyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzdiNTBkNmU2YmY5MDdmYTRlM2M0NGY0NjVjZDJjNGY3OTEyNGI1NzAzYTJkZjIyZmFjNjM3NmIxYjkxNzAzY2YiCiAgICB9CiAgfQp9"),
            new Tuple<>("Earthworm", "ewogICJ0aW1lc3RhbXAiIDogMTY5NzQ3MDQ1OTc0NywKICAicHJvZmlsZUlkIiA6ICIyNTBlNzc5MjZkNDM0ZDIyYWM2MTQ4N2EyY2M3YzAwNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJMdW5hMTIxMDUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQwM2JhNDAyN2EzMzNkOGQyZmQzMmFiNTlkMWNmZGJhYTdkOTA4ZDgwZDIzODFkYjJhNjljYmU2NTQ1MGFkOCIKICAgIH0KICB9Cn0"),
            new Tuple<>("Fly", "ewogICJ0aW1lc3RhbXAiIDogMTY5Njk0NTA2MzI4MSwKICAicHJvZmlsZUlkIiA6ICJjN2FmMWNkNjNiNTE0Y2YzOGY4NWQ2ZDUxNzhjYThlNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJtb25zdGVyZ2FtZXIzMTUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWQ5MGU3Nzc4MjZhNTI0NjEzNjhlMjZkMWIyZTE5YmZhMWJhNTgyZDYwMjQ4M2U1NDVmNDEyNGQwZjczMTg0MiIKICAgIH0KICB9Cn0"),
            new Tuple<>("Locust", "ewogICJ0aW1lc3RhbXAiIDogMTY5NzU1NzA3NzAzNywKICAicHJvZmlsZUlkIiA6ICI0YjJlMGM1ODliZjU0ZTk1OWM1ZmJlMzg5MjQ1MzQzZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJfTmVvdHJvbl8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGIyNGE0ODJhMzJkYjFlYTc4ZmI5ODA2MGIwYzJmYTRhMzczY2JkMThhNjhlZGRkZWI3NDE5NDU1YTU5Y2RhOSIKICAgIH0KICB9Cn0"),
            new Tuple<>("Mite", "ewogICJ0aW1lc3RhbXAiIDogMTY5Njg3MDQxOTcyNSwKICAicHJvZmlsZUlkIiA6ICJkYjYzNWE3MWI4N2U0MzQ5YThhYTgwOTMwOWFhODA3NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJFbmdlbHMxNzQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmU2YmFmNjQzMWE5ZGFhMmNhNjA0ZDVhM2MyNmU5YTc2MWQ1OTUyZjA4MTcxNzRhNGZlMGI3NjQ2MTZlMjFmZiIKICAgIH0KICB9Cn0"),
            new Tuple<>("Mosquito", "ewogICJ0aW1lc3RhbXAiIDogMTY5Njk0NTAyOTQ2MSwKICAicHJvZmlsZUlkIiA6ICI3NTE0NDQ4MTkxZTY0NTQ2OGM5NzM5YTZlMzk1N2JlYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGFua3NNb2phbmciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTJhOWZlMDViYzY2M2VmY2QxMmU1NmEzY2NjNWVjMDM1YmY1NzdiNzg3MDg1NDhiNmY0ZmZjZjFkMzBlY2NmZSIKICAgIH0KICB9Cn0"),
            new Tuple<>("Moth", "ewogICJ0aW1lc3RhbXAiIDogMTY5Njg3MDQwNTk1NCwKICAicHJvZmlsZUlkIiA6ICJiMTUyZDlhZTE1MTM0OWNmOWM2NmI0Y2RjMTA5NTZjOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaXNxdW90aCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82NTQ4NWM0YjM0ZTViNTQ3MGJlOTRkZTEwMGU2MWY3ODE2ZjgxYmM1YTExZGZkZjBlY2NmODkwMTcyZGE1ZDBhIgogICAgfQogIH0KfQ"),
            new Tuple<>("Rat", "ewogICJ0aW1lc3RhbXAiIDogMTYxODQxOTcwMTc1MywKICAicHJvZmlsZUlkIiA6ICI3MzgyZGRmYmU0ODU0NTVjODI1ZjkwMGY4OGZkMzJmOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCdUlJZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYThhYmI0NzFkYjBhYjc4NzAzMDExOTc5ZGM4YjQwNzk4YTk0MWYzYTRkZWMzZWM2MWNiZWVjMmFmOGNmZmU4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
            new Tuple<>("Slug", "ewogICJ0aW1lc3RhbXAiIDogMTY5NzQ3MDQ0MzA4MiwKICAicHJvZmlsZUlkIiA6ICJkOGNkMTNjZGRmNGU0Y2IzODJmYWZiYWIwOGIyNzQ4OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJaYWNoeVphY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2E3OWQwZmQ2NzdiNTQ1MzA5NjExMTdlZjg0YWRjMjA2ZTJjYzUwNDVjMTM0NGQ2MWQ3NzZiZjhhYzJmZTFiYSIKICAgIH0KICB9Cn0")
    );

    @Getter
    @Setter
    public static class Pest {
        int amount;
        int plotNumber;

        public Pest(int amount, int plotNumber) {
            this.amount = amount;
            this.plotNumber = plotNumber;
        }

        public void addPest() {
            amount++;
        }
    }

    private static PestsDestroyer instance;

    private final ArrayList<Pest> pestsMap = new ArrayList<>();

    @Getter
    private int amountOfPests = 0;

    public static PestsDestroyer getInstance() {
        if (instance == null) {
            instance = new PestsDestroyer();
        }
        return instance;
    }

    private boolean enabled = false;

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isRunning() {
        return enabled;
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
        if (enabled) return;
        enabled = true;
    }

    @Override
    public void stop() {
        if (!enabled) return;
        enabled = false;
    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.enablePestsDestroyer;
    }

    private final Pattern pestPattern = Pattern.compile("GROSS! A Pest has appeared in Plot - (\\d+)!");

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText().trim());
        Matcher matcher = pestPattern.matcher(message);
        if (!matcher.matches()) {
            return;
        }
        String plot = matcher.group(1);
        int plotNumber = -1;
        try {
            plotNumber = Integer.parseInt(plot);
        } catch (Exception e) {
            LogUtils.sendError("[Pests Destroyer] Failed to parse plot number: " + plot);
            return;
        }
        int finalPlotNumber = plotNumber;
        Optional<Pest> pestOptional = pestsMap.stream().filter(p -> p.plotNumber == finalPlotNumber).findFirst();
        if (pestOptional.isPresent()) {
            Pest pest1 = pestOptional.get();
            pest1.addPest();
            LogUtils.sendDebug("PlotNumber: " + finalPlotNumber + " count: " + pest1.amount);
            return;
        }
        Pest newPest = new Pest(1, finalPlotNumber);
        pestsMap.add(newPest);
        LogUtils.sendDebug("PlotNumber: " + finalPlotNumber + " count: " + newPest.amount);
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isToggled()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        List<Entity> pests = mc.theWorld.loadedEntityList.stream().filter(entity -> {
            if (entity instanceof EntityArmorStand) {
                ItemStack itemStack = ((EntityArmorStand) entity).getEquipmentInSlot(4);
                if (itemStack != null && itemStack.hasTagCompound()) {
                    String displayName = itemStack.getTagCompound().toString();
                    if (!displayName.contains("display:")) {
                        return this.pests.stream().anyMatch(pest -> displayName.contains(pest.getSecond()));
                    }
                }
            }
            return false;
        }).collect(Collectors.toList());

        for (Entity entity : pests) {
            AxisAlignedBB boundingBox = new AxisAlignedBB(entity.posX - 0.5, entity.posY + entity.getEyeHeight() - 0.35, entity.posZ - 0.5, entity.posX + 0.5, entity.posY + entity.getEyeHeight() + 0.65, entity.posZ + 0.5);
            double d0 = Minecraft.getMinecraft().getRenderManager().viewerPosX;
            double d1 = Minecraft.getMinecraft().getRenderManager().viewerPosY;
            double d2 = Minecraft.getMinecraft().getRenderManager().viewerPosZ;
            boundingBox = boundingBox.offset(-d0, -d1, -d2);
            if (FarmHelperConfig.pestsESP) {
                Color color = FarmHelperConfig.pestsESPColor.toJavaColor();
                if (mc.thePlayer.canEntityBeSeen(entity)) {
                    color = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(50, color.getAlpha()));
                }
                float distance = mc.thePlayer.getDistanceToEntity(entity);
                if (distance > 5) {
                    try {
                        ItemStack itemStack = ((EntityArmorStand) entity).getEquipmentInSlot(4);
                        String pestName = this.pests.stream().filter(pest -> itemStack.getTagCompound().toString().contains(pest.getSecond())).findFirst().get().getFirst();
                        RenderUtils.drawText(pestName, entity.posX, entity.posY + entity.getEyeHeight() + 0.65 + 0.5, entity.posZ, 1 + (float) Math.min((distance / 20), 3));
                    } catch (Exception ignored) {
                    }
                }
                RenderUtils.drawBox(boundingBox, color);

            }
            if (FarmHelperConfig.pestsTracers) {
                RenderUtils.drawTracer(new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ), FarmHelperConfig.pestsTracersColor.toJavaColor());
            }
        }

        if (!FarmHelperConfig.highlightPlotWithPests) return;

//        for (Map.Entry<Integer, List<Tuple<Integer, Integer>>> chunk : PlotUtils.getPLOTS().entrySet()) {
//            List<Tuple<Integer, Integer>> chunks = chunk.getValue();
//            int plotNumber = chunk.getKey();
//            AxisAlignedBB boundingBox = new AxisAlignedBB(chunks.get(0).getFirst() * 16, 66, chunks.get(0).getSecond() * 16, chunks.get(chunks.size() - 1).getFirst() * 16 + 16, 80, chunks.get(chunks.size() - 1).getSecond() * 16 + 16);
//            double d0 = Minecraft.getMinecraft().getRenderManager().viewerPosX;
//            double d1 = Minecraft.getMinecraft().getRenderManager().viewerPosY;
//            double d2 = Minecraft.getMinecraft().getRenderManager().viewerPosZ;
//            int centerX = (int) (boundingBox.minX + (boundingBox.maxX - boundingBox.minX) / 2);
//            int centerZ = (int) (boundingBox.minZ + (boundingBox.maxZ - boundingBox.minZ) / 2);
//
//            boundingBox = boundingBox.offset(-d0, -d1, -d2);
//            RenderUtils.drawBox(boundingBox, FarmHelperConfig.plotHighlightColor.toJavaColor());
//            RenderUtils.drawText(plotNumber + "", centerX, 80, centerZ, 1);
//        }

        for (Pest pest : pestsMap) {
            int plotNumber = pest.getPlotNumber();
            List<Tuple<Integer, Integer>> chunks = PlotUtils.getPlotBasedOnNumber(plotNumber);
            AxisAlignedBB boundingBox = new AxisAlignedBB(chunks.get(0).getFirst() * 16, 66, chunks.get(0).getSecond() * 16, chunks.get(chunks.size() - 1).getFirst() * 16 + 16, 80, chunks.get(chunks.size() - 1).getSecond() * 16 + 16);
            double d0 = Minecraft.getMinecraft().getRenderManager().viewerPosX;
            double d1 = Minecraft.getMinecraft().getRenderManager().viewerPosY;
            double d2 = Minecraft.getMinecraft().getRenderManager().viewerPosZ;
            float centerX = (float) (boundingBox.minX + (boundingBox.maxX - boundingBox.minX) / 2);
            float centerZ = (float) (boundingBox.minZ + (boundingBox.maxZ - boundingBox.minZ) / 2);
            boundingBox = boundingBox.offset(-d0, -d1, -d2);
            RenderUtils.drawBox(boundingBox, FarmHelperConfig.plotHighlightColor.toJavaColor());
            int numberOfPests = pest.getAmount();
            RenderUtils.drawText("Pests in plot: " + numberOfPests, centerX, 80, centerZ, 1);
        }
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isToggled()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        Entity entity = event.entity;
        LogUtils.sendDebug("[Pests Destroyer] Entity died: " + entity.getName() + " at: " + entity.getPosition());
        int plotNumber = PlotUtils.getPlotNumberBasedOnLocation(entity.getPosition());
        if (plotNumber == -1) {
            LogUtils.sendError("[Pests Destroyer] Failed to get plot number for entity: " + entity.getName() + " at: " + entity.getPosition());
            return;
        }
        Optional<Pest> pestOptional = pestsMap.stream().filter(p -> p.plotNumber == plotNumber).findFirst();
        if (pestOptional.isPresent()) {
            Pest pest = pestOptional.get();
            pest.amount--;
            if (pest.amount <= 0) {
                pestsMap.remove(pest);
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isToggled()) return;
        if (event.phase != TickEvent.Phase.START) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        List<String> scoreBoard = ScoreboardUtils.getScoreboardLines();

        for (String line : scoreBoard) {
            String withoutColors = StringUtils.stripControlCodes(line);
            String clean = ScoreboardUtils.cleanSB(withoutColors);
            if (line.contains("ൠ")) {
                String[] split = clean.split(" ");
                try {
                    String pests = split[split.length - 1].replace("x", "").trim();
                    int pestsAmount = Integer.parseInt(pests);
                    if (pestsAmount != amountOfPests) {
                        amountOfPests = pestsAmount;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (line.contains("Garden") || line.contains("Plot")) {
                amountOfPests = 0;
            }
        }
        if (amountOfPests == 0) {
            pestsMap.clear();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        pestsMap.clear();
    }
}
