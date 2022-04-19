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
    public static boolean debug = true;
    public static boolean compactDebug = true;
    public static boolean webhookLog = false;
    public static String webhookUrl = "";
    public static boolean webhookStatus = false;
    public static Integer statusTime = 60;

    public static void setConfig(CropEnum crop, FarmEnum farm, boolean resync, boolean debug, boolean compactDebug, boolean webhookLog, String webhookUrl, Boolean webhookStatus, Integer statusTime) {
        CropType = crop;
        FarmType = farm;
        Config.resync = resync;
        Config.debug = debug;
        Config.compactDebug = compactDebug;
        Config.webhookLog = webhookLog;
        Config.webhookUrl = webhookUrl;
        Config.webhookStatus = webhookStatus;
        Config.statusTime = statusTime;
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
            bufferedWriter.write("\n" + webhookStatus);
            bufferedWriter.write("\n" + statusTime);
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
                (url = bufferedReader.readLine()) != null ? url : "",
                Boolean.parseBoolean(bufferedReader.readLine()),
                Integer.parseInt(bufferedReader.readLine())
            );
            bufferedReader.close();
        } catch(Exception e) {
            throw new Exception();
        }
    }
}