package FarmHelper.Utils;

import FarmHelper.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import scala.sys.process.ProcessBuilderImpl;

import java.io.*;
import java.util.Random;

public class Utils {

    public static void drawString(String text, int x, int y, float size, int color) {
        GlStateManager.scale(size,size,size);
        //GL11.glScalef();
        float mSize = (float)Math.pow(size,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString(text,Math.round(x / size),Math.round(y / size),color);
        GlStateManager.scale(mSize,mSize,mSize);
        //GL11.glScalef(mSize,mSize,mSize);

    }

    public static void saveConfig(Config c){
        try {
            FileOutputStream fileOut =
                    new FileOutputStream("/tmp/config.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(c);
            out.close();
            fileOut.close();
            System.out.print("Serialized data is saved in /tmp/config.ser");
        } catch (IOException i) {
            System.out.print("Error");
            i.printStackTrace();
        }
    }


    public static Config readConfig() {
        try {
            Config c = null;

            FileInputStream fileIn = new FileInputStream("/tmp/config.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            c = (Config) in.readObject();
            in.close();
            fileIn.close();

            return c;
        } catch (IOException i) {
            i.printStackTrace();
            return null;
        } catch (ClassNotFoundException c) {
            System.out.println("class not found");
            c.printStackTrace();
            return null;
        }
    }

    public static void drawHorizontalLine(int startX, int endX, int y, int color)
    {
        if (endX < startX)
        {
            int i = startX;
            startX = endX;
            endX = i;
        }

        Gui.drawRect(startX, y, endX + 1, y + 1, color);
    }

    public static void drawVerticalLine(int x, int startY, int endY, int color)
    {
        if (endY < startY)
        {
            int i = startY;
            startY = endY;
            endY = i;
        }

        Gui.drawRect(x, startY + 1, x + 1, endY, color);
    }
    public static float get360RotationYaw(){
        return Minecraft.getMinecraft().thePlayer.rotationYaw > 0?
                (Minecraft.getMinecraft().thePlayer.rotationYaw % 360) :
                (Minecraft.getMinecraft().thePlayer.rotationYaw < 360f ? 360 - (-Minecraft.getMinecraft().thePlayer.rotationYaw % 360)  :  360 + Minecraft.getMinecraft().thePlayer.rotationYaw);
    }
    public static void rotateTo(final int rotation360){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (get360RotationYaw() != rotation360) {
                        if(Math.abs(rotation360 - get360RotationYaw()) < 2) {
                            Minecraft.getMinecraft().thePlayer.rotationYaw = (int)(Minecraft.getMinecraft().thePlayer.rotationYaw + (rotation360 - get360RotationYaw()));
                            break;
                        }
                        Minecraft.getMinecraft().thePlayer.rotationYaw += 0.5f + nextInt(6)/10.0;
                        try {
                            Thread.sleep(2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

    }
    public static void rotateClockwise(final int rotationClockwise){
        new Thread(new Runnable() {
            @Override
            public void run() {
                int initialYaw = (int)Minecraft.getMinecraft().thePlayer.rotationYaw;
                while (Minecraft.getMinecraft().thePlayer.rotationYaw != initialYaw + rotationClockwise) {
                    if(Math.abs(Minecraft.getMinecraft().thePlayer.rotationYaw - initialYaw + rotationClockwise) < 2) {
                        Minecraft.getMinecraft().thePlayer.rotationYaw = (int)(Minecraft.getMinecraft().thePlayer.rotationYaw + rotationClockwise);
                        break;
                    }

                    Minecraft.getMinecraft().thePlayer.rotationYaw += 0.1f + nextInt(5)/5.0;
                    try {
                        Thread.sleep(2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();

    }
    public static float getActualRotationYaw(){ //f3
        return Minecraft.getMinecraft().thePlayer.rotationYaw > 0?
                (Minecraft.getMinecraft().thePlayer.rotationYaw % 360 > 180 ? -(180 - (Minecraft.getMinecraft().thePlayer.rotationYaw % 360 - 180)) :  Minecraft.getMinecraft().thePlayer.rotationYaw % 360  ) :
                (-Minecraft.getMinecraft().thePlayer.rotationYaw % 360 > 180 ? (180 - (-Minecraft.getMinecraft().thePlayer.rotationYaw % 360 - 180))  :  -(-Minecraft.getMinecraft().thePlayer.rotationYaw % 360));
    }
    public static int nextInt(int upperbound){
        Random r = new Random();
        return r.nextInt(upperbound);
    }

}
