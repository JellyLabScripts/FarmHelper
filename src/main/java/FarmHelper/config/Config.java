package FarmHelper.config;

import javax.annotation.Nullable;
import java.io.Serializable;

public class Config implements Serializable {


    public static CROP CropType;
    public static ANGLE Angle;
    public static FARM FarmType;

    public Config(CROP crop, FARM farm, ANGLE angle){
        CropType = crop;
        FarmType = farm;
        Angle = angle;
    }


    public enum CROP {
        WHEAT,
        NETHERWART,
        POTATO,
        CARROT
    }
    public enum FARM {
        LAYERED,
        VERTICAL
    }

    public enum ANGLE{
        A0,
        A90,
        A180,
        AN90,
    }
}



