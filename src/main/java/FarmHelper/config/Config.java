package FarmHelper.config;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Config{

    public static CropEnum CropType = CropEnum.NETHERWART;
    public static FarmEnum FarmType = FarmEnum.LAYERED;
    public static boolean rotateAfterTeleport = false;
    public static boolean inventoryPriceCalculator = false;
    public static boolean profitCalculator = false;
    public static boolean resync = false;

    public static AngleEnum Angle = AngleEnum.A0;

    public static void setConfig(CropEnum crop, FarmEnum farm, boolean rotateAfterTeleport,
                                 boolean inventoryPriceCalculator, boolean profitCalculator, boolean resync){
        CropType = crop;
        FarmType = farm;
        Config.rotateAfterTeleport = rotateAfterTeleport;
        Config.inventoryPriceCalculator = inventoryPriceCalculator;
        Config.profitCalculator = profitCalculator;
        Config.resync = resync;
    }
    public static void writeConfig(){
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("config.txt"));
            bufferedWriter.write("\n" + CropType.ordinal());
            bufferedWriter.write("\n" + FarmType.ordinal());
            bufferedWriter.write("\n" + rotateAfterTeleport);
            bufferedWriter.write("\n" + inventoryPriceCalculator);
            bufferedWriter.write("\n" + profitCalculator);
            bufferedWriter.write("\n" + resync);
            bufferedWriter.close();
        }catch(Exception e){
            e.printStackTrace();
        }

    }
    public static void readConfig () throws Exception {
        try{
            BufferedReader bufferedReader = new BufferedReader(new FileReader("config.txt"));
            bufferedReader.readLine();
            setConfig(CropEnum.values()[Integer.parseInt(bufferedReader.readLine())],
                    FarmEnum.values()[Integer.parseInt(bufferedReader.readLine())],
                    Boolean.parseBoolean(bufferedReader.readLine()),
                    Boolean.parseBoolean(bufferedReader.readLine()),
                    Boolean.parseBoolean(bufferedReader.readLine()),
                    Boolean.parseBoolean(bufferedReader.readLine()));
            bufferedReader.close();

        }catch(Exception e){
            throw new Exception();
        }
    }

}



