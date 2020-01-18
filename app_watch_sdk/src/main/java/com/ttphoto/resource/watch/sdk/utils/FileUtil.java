package com.ttphoto.resource.watch.sdk.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

public class FileUtil {

    public static boolean makeSureFolderOfFileExist(String path) {
        int pos = path.lastIndexOf("/");
        String folder = path.substring(0, pos);
        return makeSureFolderExist(folder);
    }

    public static boolean makeSureFolderExist(String path) {
        File dir = new File(path);

        if (!dir.exists()) {
            return dir.mkdirs();
        } else {
            return dir.isDirectory();
        }

    }

    public static void writeStringToFile(final String path, final String str) {

        makeSureFolderOfFileExist(path);

        FileWriter fw = null;
        try {
            fw = new FileWriter(path);
            fw.write(str);
            fw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static void appendStringToFile(final String path, final String str) {

        makeSureFolderOfFileExist(path);

        FileWriter fw = null;
        try {
            fw = new FileWriter(path, true);
            fw.write(str);
            fw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static String readStringFromFile(final String path) {
        File file = new File(path);
        if (!file.exists()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String data;
            while ((data = br.readLine()) != null) {
                builder.append(data);
            }
        } catch (Exception e) {
            Log.e("FileUtil", "readStringFromFile exception:" + e.getMessage());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }

        return builder.toString();
    }

}
