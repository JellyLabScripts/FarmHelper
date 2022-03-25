package FarmHelper.config;

import javax.annotation.Nullable;
import java.io.Serializable;

public class Config implements Serializable {


    public static CropEnum CropType;
    public static AngleEnum Angle;
    public static FarmEnum FarmType;
    public static boolean rotateAfterTeleport = false;
    public static boolean inventoryPriceCalculator = false;
    public static boolean profitCalculator = false;
    public static boolean resync = false;



    public static void setConfig(CropEnum crop, FarmEnum farm, AngleEnum angle){
        CropType = crop;
        FarmType = farm;
        Angle = angle;
    }

}



