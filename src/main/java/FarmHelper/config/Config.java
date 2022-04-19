package FarmHelper.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class Config {
    public static AngleEnum Angle = AngleEnum.A0;
    public static CropEnum CropType = CropEnum.NETHERWART;
    public static FarmEnum FarmType = FarmEnum.LAYERED;
    public static boolean resync = true;
    public static boolean jacobFailsafe = true;
    public static int jacobThreshold = 400000;
    public static boolean webhookLog = false;
    public static String webhookUrl = "";
    public static Integer statusTime = 10;
    public static boolean debug = false;


    public static void setConfig(CropEnum crop, FarmEnum farm, boolean resync, boolean jacobFailsafe, int jacobThreshold, boolean webhookLog, String webhookUrl, Integer statusTime, boolean debug) {
        CropType = crop;
        FarmType = farm;
        Config.resync = resync;
        Config.jacobFailsafe = jacobFailsafe;
        Config.jacobThreshold = jacobThreshold;
        Config.webhookLog = webhookLog;
        Config.webhookUrl = webhookUrl;
        Config.statusTime = statusTime;
        Config.debug = debug;
    }

    public static void writeConfig() {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("config.txt"));
            bufferedWriter.write("\n" + CropType.ordinal());
            bufferedWriter.write("\n" + FarmType.ordinal());
            bufferedWriter.write("\n" + resync);
            bufferedWriter.write("\n" + jacobFailsafe);
            bufferedWriter.write("\n" + jacobThreshold);
            bufferedWriter.write("\n" + webhookLog);
            bufferedWriter.write("\n" + webhookUrl);
            bufferedWriter.write("\n" + statusTime);
            bufferedWriter.write("\n" + debug);
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
                Integer.parseInt(bufferedReader.readLine()),
                Boolean.parseBoolean(bufferedReader.readLine()),
                (url = bufferedReader.readLine()) != null ? url : "",
                Integer.parseInt(bufferedReader.readLine()),
                Boolean.parseBoolean(bufferedReader.readLine())
            );
            bufferedReader.close();
        } catch(Exception e) {
            throw new Exception();
        }
    }
}