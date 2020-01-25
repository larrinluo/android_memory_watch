// MIT License
//
// Copyright (c) 2019 larrinluo
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
// Created by larrin luo on 2020-01-11.
//
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
