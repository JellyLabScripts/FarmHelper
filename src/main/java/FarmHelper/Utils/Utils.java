package FarmHelper.Utils;

import FarmHelper.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;
import scala.sys.process.ProcessBuilderImpl;

import java.io.*;

public class Utils {

    public static void drawString(String text, int x, int y, float size, int color) {
        GL11.glScalef(size,size,size);
        float mSize = (float)Math.pow(size,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString(text,Math.round(x / size),Math.round(y / size),color);
        GL11.glScalef(mSize,mSize,mSize);
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

}
