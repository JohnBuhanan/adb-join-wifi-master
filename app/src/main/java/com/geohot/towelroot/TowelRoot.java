package com.geohot.towelroot;

import android.os.Bundle;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;


//TODO flash recovery image. Do i need to move image there before i do a DD command?
//TODO install app? connect to wifi afterwards?
public class TowelRoot extends WifiActivity {
    private static final String TAG = TowelRoot.class.getSimpleName();

    private static final String MOD_STRING = "1337 0 0 0 4 0";

    public native String rootTheShit(String str);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        connectToWifi();

        String rootResult = rootPhone();

        if (rootResult.startsWith("Thank")) { // Success
            String flashResult = flashRecovery();
            String rebootResult = rebootPhone();
        } else
            Log.e(TAG, "Root failed!");
    }

    private String flashRecovery() {
        String backupDirectory = "/storage/external_SD/clockworkmod/backup/";
        String getDateDirectoryCommand = "ls " + backupDirectory;

        String dateDirectory = sudoForResult(getDateDirectoryCommand);
        dateDirectory = dateDirectory.replaceAll("\n", "");

        if (dateDirectory.isEmpty()) {
            Log.e(TAG, "Date directory was empty!");
            return dateDirectory;
        }

        String ddRecoveryImageCommand = "dd if=" + backupDirectory + dateDirectory + "/recovery.img" + " of=/dev/block/mmcblk0p16";
        String ddResult = sudoForResult(ddRecoveryImageCommand);
        Log.d(TAG, ddResult);

        return ddResult;
    }

    private String rebootPhone() {
        return sudoForResult("reboot");
    }

    public String rootPhone() {
        String msg = rootTheShit(MOD_STRING);
        Log.d(TAG, msg);
        return msg;
    }

    public static String sudoForResult(String... strings) {
        String res = "";
        DataOutputStream outputStream = null;
        InputStream response = null;
        try {
            Process su = Runtime.getRuntime().exec("su");
            outputStream = new DataOutputStream(su.getOutputStream());
            response = su.getInputStream();

            for (String s : strings) {
                outputStream.writeBytes(s + "\n");
                outputStream.flush();
            }

            outputStream.writeBytes("exit\n");
            outputStream.flush();

            try {
                su.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            res = readFully(response);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return res;
    }

    public static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString("UTF-8");
    }


    static {
        System.loadLibrary("exploit");
    }
}