package com.ttphoto.resource.watch.sdk.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;

public class Utils {

    public static void closeScilently(Closeable it) {
        if (it != null) {
            try {
                it.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void waitScilently(Object obj, int timeout) {
        try {
            obj.wait(timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readProcFile(String filePath, int pid) {
        FileInputStream inputStream = null;
        try {
            String path = String.format("/proc/%d/%s", pid, filePath);
            File file = new File(path);

            if (file.exists()) {
                inputStream = new FileInputStream(file);
                int bytes = 0;
                byte[] buffer = new byte[1024]; //无法预先获取文件长度，动态调整buffer大小
                while (true) {
                    int b = inputStream.read(buffer, bytes, buffer.length - bytes);
                    if (b <= 0)
                        break;

                    bytes += b;
                    if (bytes == buffer.length) { //need expend buffer
                        byte[] newBuffer = new byte[buffer.length + 1024];
                        System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                        buffer = newBuffer;
                    }
                }

                if (bytes > 0) {
                    return new String(buffer);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.closeScilently(inputStream);
        }

        return null;
    }
}
