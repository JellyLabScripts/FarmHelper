package com.jelly.farmhelperv2.util.helper;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.PathCalculationResult;
import baritone.pathing.calc.FlyAStar;
import baritone.pathing.movement.CalculationContext;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.BlockUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.RenderUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

// Not yet finished :(
public class FlyPathfinder {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static FlyPathfinder instance;

    public static FlyPathfinder getInstance() {
        if (instance == null) {
            instance = new FlyPathfinder();
        }
        return instance;
    }

    @Getter
    @Setter
    private Goal goal;
    private CalculationContext context;
    @Getter
    private final List<BlockPos> pathBlocks = new ArrayList<>();
    private static final RotationHandler rotation = RotationHandler.getInstance();

    public List<BlockPos> getPathTo(Goal goal) {
        return getPathTo(goal, false);
    }

    public List<BlockPos> getPathTo(Goal goal, boolean onlyTurns) {
        if (context == null) {
            context = new CalculationContext(BaritoneAPI.getProvider().getPrimaryBaritone(), true);
        }
        this.goal = goal;
        List<BlockPos> tempList = new ArrayList<>();
        BlockPos playerPos = BlockUtils.getRelativeBlockPos(0, 0, 0);
        FlyAStar finder = new FlyAStar(playerPos.getX(), playerPos.getY(), playerPos.getZ(), goal, context);
        PathCalculationResult calcResult = finder.calculate(1000, 1000);
        Optional<IPath> path = calcResult.getPath();
        path.ifPresent(iPath -> tempList.addAll(iPath.positions()));
        pathBlocks.clear();
        if (onlyTurns) {
            pathBlocks.addAll(getOnlyTurns3(getOnlyTurns(tempList)));
        } else {
            pathBlocks.addAll(tempList);
        }
        return pathBlocks;
    }

    enum Direction {
        X,
        Y,
        Z
    }

    public List<BlockPos> getOnlyTurns(List<BlockPos> list) {
        if (list.isEmpty()) {
            LogUtils.sendDebug("Path is empty");
            return new ArrayList<>();
        }
        ArrayList<BlockPos> turns = new ArrayList<>();
        BlockPos lastPos = list.get(0);
        turns.add(lastPos);
        Direction lastDirection = Direction.X;
        for (BlockPos pos : list) {
            if (pos.getX() != lastPos.getX()) {
                if (lastDirection != Direction.X) {
                    turns.add(lastPos);
                }
                lastDirection = Direction.X;
            } else if (pos.getY() != lastPos.getY()) {
                if (lastDirection != Direction.Y) {
                    turns.add(lastPos);
                }
                lastDirection = Direction.Y;
            } else if (pos.getZ() != lastPos.getZ()) {
                if (lastDirection != Direction.Z) {
                    turns.add(lastPos);
                }
                lastDirection = Direction.Z;
            }
            lastPos = pos;
        }
        turns.add(lastPos);
        return new ArrayList<>(turns);
    }

    public List<BlockPos> getOnlyTurns2(List<BlockPos> list) {
        if (list.size() < 3) return list;
        List<BlockPos> tempList = new ArrayList<>();
        tempList.add(list.get(0));
        for (int i = 1; i < list.size() - 1; i++) {
            BlockPos current = list.get(i);
            BlockPos next = list.get(i + 1);
            BlockPos previous = list.get(i - 1);
            if (current.getX() != next.getX() && current.getX() != previous.getX()) {
                tempList.add(current);
            } else if (current.getY() != next.getY() && current.getY() != previous.getY()) {
                tempList.add(current);
            } else if (current.getZ() != next.getZ() && current.getZ() != previous.getZ()) {
                tempList.add(current);
            }
        }
        tempList.add(list.get(list.size() - 1));
        return tempList;
    }

    public List<BlockPos> getOnlyTurns3(List<BlockPos> list) {
        if (list.size() < 3) return list;
        List<BlockPos> tempList = new ArrayList<>();
        tempList.add(list.get(0));
        BlockPos startOfShattering = null;
        for (int i = 0; i < list.size() - 2; i++) {
            BlockPos current = list.get(i);
            BlockPos next = list.get(i + 1);
            if (Math.sqrt(current.distanceSq(next.getX(), next.getY(), next.getZ())) <= 2.5) {
                if (startOfShattering == null) {
                    startOfShattering = current;
                    continue;
                }
                MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(
                        new Vec3(startOfShattering.getX() + 0.5, startOfShattering.getY() + 0.5, startOfShattering.getZ() + 0.5),
                        new Vec3(next.getX() + 0.5, next.getY() + 0.5, next.getZ() + 0.5), false, true, false);
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    tempList.add(startOfShattering);
                    tempList.add(current);
                    startOfShattering = current;
                }
            } else {
                if (startOfShattering != null) {
                    tempList.add(startOfShattering);
                    startOfShattering = null;
                }
                tempList.add(current);
            }
        }
        tempList.add(list.get(list.size() - 1));
        return tempList;
    }

    public boolean hasGoal() {
        return goal != null;
    }

    public void stop() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        goal = null;
        pathBlocks.clear();
    }

    public boolean isPathing() {
        return BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing();
    }

    public boolean isRunning() {
        return !pathBlocks.isEmpty();
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!FarmHelperConfig.debugMode) return;
        if (pathBlocks.isEmpty()) return;
        for (BlockPos pos : pathBlocks) {
            int value = 255 - 50 * pathBlocks.indexOf(pos);
            if (value < 0) value = 0;
            RenderUtils.drawBlockBox(pos, new Color(value, 0, 0, 150));
        }
    }

    public boolean w1, w2, w3, w4 = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (pathBlocks.isEmpty()) {
            if (isPathing()) {
                stop();
                LogUtils.sendDebug("Fly pathing stopped");
            }
            return;
        }
        // get distance to X, Y and Z of the next block
        double distanceX = pathBlocks.get(0).getX() - mc.thePlayer.posX + 0.5;
        double distanceY = pathBlocks.get(0).getY() - mc.thePlayer.posY + 0.5;
        double distanceZ = pathBlocks.get(0).getZ() - mc.thePlayer.posZ + 0.5;
        // swap X and Z based on looking direction
        float yaw = mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f;
        double relativeDistanceX = distanceX * Math.cos(yaw) + distanceZ * Math.sin(yaw);
        double relativeDistanceZ = -distanceX * Math.sin(yaw) + distanceZ * Math.cos(yaw);
        double relativeMotionX = 1*(mc.thePlayer.motionX * Math.cos(yaw) - mc.thePlayer.motionZ * Math.sin(yaw));
        double relativeMotionZ = -mc.thePlayer.motionX * Math.sin(yaw) + mc.thePlayer.motionZ * Math.cos(yaw);
        if (mc.thePlayer.onGround) {
            stop();
            KeyBindUtils.stopMovement();
            return;
        }
        double distance2d = Math.abs(Math.sqrt(relativeDistanceX * relativeDistanceX + relativeDistanceZ * relativeDistanceZ));
        if (distance2d < 0.5) {
            pathBlocks.remove(0);
            if (!rotation.isRotating() && pathBlocks.size() > 2) {
                Vec3 target = new Vec3(pathBlocks.get(0).getX() + 0.5, pathBlocks.get(0).getY() + 0.5, pathBlocks.get(0).getZ() + 0.5);
                rotation.easeTo(
                        new RotationConfiguration(
                                new Rotation(rotation.getRotation(target, true).getYaw(), rotation.getRotation(target, true).getPitch()),
                                500, null
                        )
                );
            }
            KeyBindUtils.stopMovement();
        } else {
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSprint, relativeDistanceZ > 11);
            mc.thePlayer.capabilities.isFlying = true;
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindJump, distanceY > 0.25);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, distanceY < -0.25);
            mc.thePlayer.setSprinting(relativeDistanceZ > 11);
            w1 = relativeMotionX > 0 && decelerationReached(relativeMotionX, relativeDistanceX);
            w2 = relativeMotionX < 0 && decelerationReached(relativeMotionX, relativeDistanceX);
            w3 = relativeMotionZ < 0 && decelerationReached(relativeMotionZ, relativeDistanceZ);
            w4 = relativeMotionZ > 0 && decelerationReached(relativeMotionZ, relativeDistanceZ);
            boolean o1 = relativeDistanceX < -FarmHelperConfig.flightAllowedOvershootThreshold;
            boolean o2 = relativeDistanceX > FarmHelperConfig.flightAllowedOvershootThreshold;
            boolean o3 = relativeDistanceZ < -FarmHelperConfig.flightAllowedOvershootThreshold;
            boolean o4 = relativeDistanceZ > FarmHelperConfig.flightAllowedOvershootThreshold;
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, o1);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, o2);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, o4);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, o3);
            if (w1) {
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, false);
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, true);
            }
            if (w2) {
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, false);
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, true);
            }
            if (w3) {
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, true);
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, false);
            }
            if (w4) {
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, false);
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, true);
            }
        }
    }

    public double calculateDecelerationDistance(double deceleration) { // 0.0064
        return (Math.sqrt(getPlayerSpeed())) / (2 * deceleration);
    }

    private double getPlayerSpeed() {
        return Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionY * mc.thePlayer.motionY + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    public boolean decelerationReached(double motion, double distance) {
        double reachedMotion = 0.1;
        for (int i = 0; i < decelerationDistances.size() - 1; i++) {
            if (Math.abs(motion) > reachedMotion && decelerationDistances.get(i) > Math.abs(distance)) {
                return true;
            }
            reachedMotion += 0.01;
        }
        return false;
    }

    private final ArrayList<Double> decelerationDistances = new ArrayList<Double>() {{
        add(0.039000000804662704);
        add(0.02443408966064453);
        add(0.05181508883833885);
        add(0.07919608801603317);
        add(0.10657709091901779);
        add(0.13395808637142181);
        add(0.16133908927440643);
        add(0.14573527872562408);
        add(0.18065199255943298);
        add(0.21556870639324188);
        add(0.2504854202270508);
        add(0.2854021191596985);
        add(0.3203188478946686);
        add(0.30623555183410645);
        add(0.3500385582447052);
        add(0.3918127715587616);
        add(0.433586984872818);
        add(0.475361168384552);
        add(0.5171353816986084);
        add(0.5589095950126648);
        add(0.551683783531189);
        add(0.6006367802619934);
        add(0.6486513018608093);
        add(0.69666588306427);
        add(0.7446804046630859);
        add(0.7926949262619019);
        add(0.8407094478607178);
        add(0.8887239694595337);
        add(0.8877385258674622);
        add(0.9431252479553223);
        add(0.996818482875824);
        add(1.0505117177963257);
        add(1.1042048931121826);
        add(1.157898187637329);
        add(1.211591362953186);
        add(1.2652846574783325);
        add(1.3189778327941895);
        add(1.3236711025238037);
        add(1.3859915733337402);
        add(1.444852352142334);
        add(1.5037132501602173);
        add(1.562574028968811);
        add(1.6214348077774048);
        add(1.680295705795288);
        add(1.7391564846038818);
        add(1.7980173826217651);
        add(1.8078782558441162);
        add(1.8723225593566895);
        add(1.9358859062194824);
        add(1.9994492530822754);
        add(2.0630125999450684);
        add(2.1265759468078613);
        add(2.1901392936706543);
        add(2.2537026405334473);
        add(2.3172659873962402);
        add(2.380829334259033);
        add(2.4443929195404053);
        add(2.458956241607666);
        add(2.5300827026367188);
        add(2.5979254245758057);
        add(2.6657681465148926);
        add(2.7336108684539795);
        add(2.8014533519744873);
        add(2.869296073913574);
        add(2.937138795852661);
        add(3.004981517791748);
        add(3.072824001312256);
        add(3.1406667232513428);
        add(3.1595094203948975);
        add(3.232480525970459);
        add(3.3042173385620117);
        add(3.3759541511535645);
        add(3.447690963745117);
        add(3.51942777633667);
        add(3.5911645889282227);
        add(3.6629014015197754);
        add(3.734638214111328);
        add(3.806375026702881);
        add(3.8781118392944336);
        add(3.9498486518859863);
        add(4.021585464477539);
        add(4.044322490692139);
        add(4.122203826904297);
        add(4.197484493255615);
        add(4.272765159606934);
        add(4.348045349121094);
        add(4.423326015472412);
        add(4.4986066818237305);
        add(4.573886871337891);
        add(4.649167537689209);
        add(4.724448204040527);
        add(4.7997283935546875);
        add(4.875009059906006);
        add(4.950289726257324);
        add(5.025569915771484);
        add(5.051850318908691);
        add(5.133279323577881);
        add(5.211784839630127);
        add(5.290289878845215);
        add(5.368795394897461);
        add(5.447300434112549);
        add(5.525805950164795);
        add(5.604310989379883);
        add(5.682816028594971);
        add(5.761321544647217);
        add(5.839826583862305);
        add(5.918332099914551);
        add(5.996837139129639);
        add(6.075342655181885);
        add(6.153847694396973);
        add(6.183352947235107);
        add(6.266881465911865);
        add(6.348320960998535);
        add(6.429760932922363);
        add(6.511200428009033);
        add(6.592640399932861);
        add(6.6740803718566895);
        add(6.755519866943359);
        add(6.8369598388671875);
        add(6.918399810791016);
        add(6.9998393058776855);
        add(7.081279277801514);
        add(7.162718772888184);
        add(7.244158744812012);
        add(7.32559871673584);
        add(7.40703821182251);
        add(7.439478397369385);
        add(7.520917892456055);
        add(7.607735633850098);
        add(7.691845893859863);
        add(7.775956153869629);
        add(7.8600664138793945);
        add(7.94417667388916);
        add(8.028286933898926);
        add(8.112397193908691);
        add(8.196507453918457);
        add(8.280617713928223);
        add(8.364727973937988);
        add(8.448838233947754);
        add(8.53294849395752);
        add(8.617058753967285);
        add(8.70116901397705);
        add(8.7852783203125);
    }};
}
