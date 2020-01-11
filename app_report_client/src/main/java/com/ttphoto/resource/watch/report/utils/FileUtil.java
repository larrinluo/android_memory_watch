package com.ttphoto.resource.watch.report.utils;

import java.io.File;
import java.io.FileWriter;

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

}
