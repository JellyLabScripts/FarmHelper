package FarmHelper.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class Config {
    public static AngleEnum Angle = AngleEnum.A0;
    public static CropEnum CropType = CropEnum.NETHERWART;
    public static FarmEnum FarmType = FarmEnum.LAYERED;
    public static boolean resync = false;
    public static boolean debug = false;
    public static boolean compactDebug = false;
    public static boolean webhookLog = false;
    public static String webhookUrl = "";

    public static void setConfig(CropEnum crop, FarmEnum farm, boolean resync, boolean debug, boolean compactDebug, boolean webhookLog, String webhookUrl) {
        CropType = crop;
        FarmType = farm;
        Config.resync = resync;
        Config.debug = debug;
        Config.compactDebug = compactDebug;
        Config.webhookLog = webhookLog;
        Config.webhookUrl = webhookUrl;
    }

    public static void writeConfig() {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("config.txt"));
            bufferedWriter.write("\n" + CropType.ordinal());
            bufferedWriter.write("\n" + FarmType.ordinal());
            bufferedWriter.write("\n" + resync);
            bufferedWriter.write("\n" + debug);
            bufferedWriter.write("\n" + compactDebug);
            bufferedWriter.write("\n" + webhookLog);
            bufferedWriter.write("\n" + webhookUrl);
            bufferedWriter.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void readConfig() throws Exception {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("config.txt"));
            bufferedReader.readLine();
            String url;
            setConfig(
                CropEnum.values()[Integer.parseInt(bufferedReader.readLine())],
                FarmEnum.values()[Integer.parseInt(bufferedReader.readLine())],
                Boolean.parseBoolean(bufferedReader.readLine()),
                Boolean.parseBoolean(bufferedReader.readLine()),
                Boolean.parseBoolean(bufferedReader.readLine()),
                Boolean.parseBoolean(bufferedReader.readLine()),
                (url = bufferedReader.readLine()) != null ? url : ""
            );
            bufferedReader.close();
        } catch(Exception e) {
            throw new Exception();
        }
    }
}