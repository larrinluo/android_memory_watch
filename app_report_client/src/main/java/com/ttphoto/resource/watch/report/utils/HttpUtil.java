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
package com.ttphoto.resource.watch.report.utils;

import com.ttphoto.resource.watch.sdk.utils.FileUtil;
import com.ttphoto.resource.watch.sdk.utils.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;

public class HttpUtil {

    public static byte[] getHttpResponseBytes(final String urlString) {

        ByteArrayOutputStream outputStream = null;
        HttpURLConnection httpURLConnection = null;
        InputStream stream = null;

        try {
            URL url = new URL(urlString);
            httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);

            httpURLConnection.connect();

            if (httpURLConnection.getResponseCode() != 200) {
                return null;
            }

            outputStream = new ByteArrayOutputStream();
            stream = httpURLConnection.getInputStream();

            byte[] chunk = new byte[4096];
            int bytesRead;

            while ((bytesRead = stream.read(chunk)) > 0) {
                outputStream.write(chunk, 0, bytesRead);
            }

            byte[] binaryData = outputStream.toByteArray();
            return binaryData;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            Utils.closeSilently(outputStream);
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }

            Utils.closeSilently(stream);
        }

        return null;
    }

    public static String getHttpResponse(final String urlString) {
        return getHttpResponse(urlString, 0);
    }

    public static String getHttpResponse(final String urlString, int timeout) {

        String response = null;
        HttpURLConnection httpURLConnection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(urlString);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            if (timeout != 0)
                httpURLConnection.setConnectTimeout(timeout);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);

            httpURLConnection.connect();
            StringBuilder builder = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            reader.close();

            if (httpURLConnection.getResponseCode() == 200) {
                response = builder.toString();
            }

            httpURLConnection.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (httpURLConnection != null)
                httpURLConnection.disconnect();

            Utils.closeSilently(reader);
        }

        return response;
    }

    public static String getHttpResponse(final String requireUrl, final Map<String, Object> post, final boolean needCompress) throws Exception {
        StringBuilder builder = new StringBuilder();
        for(Entry<String, Object> entry: post.entrySet()) {
            if (builder.length() > 0)
                builder.append("&");
            builder.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue().toString(),"UTF-8"));
        }

        return getHttpResponse(requireUrl, builder.toString(), needCompress);
    }

    public static String getHttpResponse(final String requireUrl, final String post, final boolean needCompress) throws Exception {

        String response = null;
        HttpURLConnection httpURLConnection = null;
        OutputStream outStream = null;
        BufferedReader reader = null;
        try {

            URL url = new URL(requireUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();

            byte[] bytes = null;
            int length = 0;

            if (post != null) {
                bytes = post.getBytes("utf-8");

                if (needCompress) {
                    bytes = Utils.gzip(bytes);
                    httpURLConnection.setRequestProperty("Content-encoding", "gzip");
                }

                httpURLConnection.setRequestProperty("content-length", "" + bytes.length);
            }

            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);

            if (bytes != null) {
                outStream = httpURLConnection.getOutputStream();
                if (outStream != null) {
                    outStream.write(bytes, 0, bytes.length);
                    outStream.flush();
                    outStream.close();
                    outStream = null;
                }
            }

            httpURLConnection.connect();
            int code = httpURLConnection.getResponseCode();
            if (code == 200) {
                StringBuilder builder = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                response = builder.toString();
            }

        } finally {

            Utils.closeSilently(outStream);
            Utils.closeSilently(reader);

            if (httpURLConnection != null)
                httpURLConnection.disconnect();
        }

        return response;
    }

    public static int postWithStatusCode(final String requireUrl, final Map<String, String> post, final boolean needCompress) {

        HttpURLConnection httpURLConnection = null;
        OutputStream outStream = null;
        BufferedReader reader = null;
        try {

            URL url = new URL(requireUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();

            byte[] bytes = null;

            if (post != null) {

                StringBuilder builder = new StringBuilder();
                for(Entry<String, String> entry: post.entrySet()) {
                    if (builder.length() > 0)
                        builder.append("&");
                    builder.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(entry.getValue(),"UTF-8"));
                }

                bytes = builder.toString().getBytes("utf-8");

                if (needCompress) {
                    bytes = Utils.gzip(bytes);
                    httpURLConnection.setRequestProperty("Content-Encoding", "gzip");
                }

                httpURLConnection.setRequestProperty("content-length", "" + bytes.length);
                httpURLConnection.setRequestProperty("Content-Type", "application/octet-stream"); //服务器自己解析，作为流上传
            }

            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);

            if (bytes != null) {
                outStream = httpURLConnection.getOutputStream();
                if (outStream != null) {
                    outStream.write(bytes, 0, bytes.length);
                    outStream.flush();
                    outStream.close();
                    outStream = null;
                }
            }

            httpURLConnection.connect();

            int code = httpURLConnection.getResponseCode();
            return code;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            Utils.closeSilently(outStream);
            Utils.closeSilently(reader);

            if (httpURLConnection != null)
                httpURLConnection.disconnect();
        }

        return -1;
    }

    /**
     *
     * @param requireUrl
     * @param form
     * @return  600 - 本地异常， 其他为服务端返回码
     */
    public static int submitFormWithStatusCode(final String requireUrl, Map<String, Object> form) {

        HttpURLConnection connection = null;
        DataOutputStream dos = null;
        BufferedReader reader = null;
        String response = null;

        String boundary = "---------------------------265001916915724";

        String lineEnd = "\r\n";
        byte[] buffer = null;

        int status = 404;

        try {
            URL url = new URL(requireUrl);
            connection = (HttpURLConnection) url.openConnection();

            // 允许向url流中读写数据
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // 启动post方法
            connection.setRequestMethod("POST");

            // 设置请求头内容
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            dos = new DataOutputStream(connection.getOutputStream());

            for (Entry<String, Object> entry : form.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String) {
                    String head = "--" + boundary + lineEnd + "Content-Disposition: form-data; name=\"" + fieldName + "\"" + lineEnd + lineEnd;
                    dos.write(head.getBytes("UTF-8"));
                    dos.write(((String) value).getBytes("UTF-8"));
                } else if (value instanceof File) {

                    // file part
                    File file = (File) value;
                    String fileMeta = "--" + boundary + lineEnd +
                            "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + file.getName() + "\"" + lineEnd +
                            "Content-Type: " + MIME.getFileMime(file.getName()) +
                            lineEnd+ lineEnd;
                    dos.write(fileMeta.getBytes("UTF-8"));

                    FileInputStream fin = null;
                    try {
                        // file stream
                        fin = new FileInputStream((File) value);
                        buffer = new byte[4096];
                        int byteread;

                        while ((byteread = fin.read(buffer)) != -1) {
                            dos.write(buffer, 0, byteread);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        Utils.closeSilently(fin);
                    }

                } else if (value instanceof byte[]) {
                    String head = "--" + boundary + lineEnd + "Content-Disposition: form-data; name=\"" + fieldName + "\"" + lineEnd + lineEnd;
                    dos.write(head.getBytes("UTF-8"));
                    dos.write((byte[]) value);
                } else {
                    String head = "--" + boundary + lineEnd + "Content-Disposition: form-data; name=\"" + fieldName + "\"" + lineEnd + lineEnd;
                    dos.write(head.getBytes("UTF-8"));
                    dos.write(value.toString().getBytes("UTF-8"));
                }

                dos.writeBytes(lineEnd);
            }

            dos.writeBytes("--" + boundary + "--");
            dos.flush();

            StringBuilder builder = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            status = connection.getResponseCode();

        } catch (Exception e) {
            e.printStackTrace();
            status = 600; // 本地错
        } finally {

            Utils.closeSilently(dos);
            Utils.closeSilently(reader);

            if (connection != null) {
                connection.disconnect();
            }
        }

        return status;

    }

    public interface FileUploadListener {
        void progress(int progress);
    }

    public static String submitForm(final String requireUrl, Map<String, Object> form) {
        return submitForm(requireUrl, form, null);
    }

    public static String submitForm(final String requireUrl, Map<String, Object> form, FileUploadListener listener) {

        HttpURLConnection connection = null;
        DataOutputStream dos = null;
        BufferedReader reader = null;
        String response = null;

        String time = StringUtil.md5("x" + System.currentTimeMillis() + "yy");
        String boundary = "---------------------------" + time;

        String lineEnd = "\r\n";
        byte[] buffer = null;

        try {
            URL url = new URL(requireUrl);
            connection = (HttpURLConnection) url.openConnection();

            // 允许向url流中读写数据
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // 启动post方法
            connection.setRequestMethod("POST");

            // 设置请求头内容
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            dos = new DataOutputStream(connection.getOutputStream());

            for (Entry<String, Object> entry : form.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String) {
                    String head = "--" + boundary + lineEnd + "Content-Disposition: form-data; name=\"" + fieldName + "\"" + lineEnd + lineEnd;
                    dos.write(head.getBytes("UTF-8"));
                    dos.write(((String) value).getBytes("UTF-8"));
                } else if (value instanceof File) {

                    // file part
                    File file = (File) value;
                    String fileMeta = "--" + boundary + lineEnd +
                            "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + file.getName() + "\"" + lineEnd +
                            "Content-Type: " + MIME.getFileMime(file.getName()) +
                            lineEnd+ lineEnd;
                    dos.write(fileMeta.getBytes("UTF-8"));

                    int byteWrited = 0;
                    int size = fieldName.length();

                    FileInputStream fin = null;
                    try {
                        // file stream
                        fin = new FileInputStream((File) value);
                        buffer = new byte[4096];int byteread;

                        while ((byteread = fin.read(buffer)) != -1) {
                            dos.write(buffer, 0, byteread);
                            byteWrited += byteread;

                            if (listener != null) {
                                int progress = (int)(byteWrited / (float) size) * 100;
                                if (progress >= 90)
                                    progress = 90;

                                listener.progress(progress);
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        Utils.closeSilently(fin);
                    }

                } else if (value instanceof byte[]) {
                    String head = "--" + boundary + lineEnd + "Content-Disposition: form-data; name=\"" + fieldName + "\"" + lineEnd + lineEnd;
                    dos.write(head.getBytes("UTF-8"));
                    dos.write((byte[]) value);
                } else {
                    String head = "--" + boundary + lineEnd + "Content-Disposition: form-data; name=\"" + fieldName + "\"" + lineEnd + lineEnd;
                    dos.write(head.getBytes("UTF-8"));
                    dos.write(value.toString().getBytes("UTF-8"));
                }

                dos.writeBytes(lineEnd);
            }

            dos.writeBytes("--" + boundary + "--");
            dos.flush();

            if (listener != null) {
                listener.progress(91);
            }

            StringBuilder builder = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            response = builder.toString();

            if (connection.getResponseCode() != 200)  {
                response = null;
            }

            if (listener != null) {
                listener.progress(100);
            }

        } catch (Exception e) {
            e.printStackTrace();
            listener.progress(-1);
        } finally {

            Utils.closeSilently(dos);
            Utils.closeSilently(reader);

            if (connection != null) {
                connection.disconnect();
            }
        }

        return response;
    }

    public static HttpURLConnection getHttpConnection(final String urlString) {

        HttpURLConnection httpURLConnection = null;

        try {
            URL url = new URL(urlString);
            httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);

            httpURLConnection.connect();

            new InputStreamReader(httpURLConnection.getInputStream());

            if (httpURLConnection.getResponseCode() != 200) {
                httpURLConnection.disconnect();
                httpURLConnection = null;
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return httpURLConnection;
    }

    public static HttpURLConnection getHttpConnection(final String urlString, final int timeout) {

        HttpURLConnection httpURLConnection = null;

        try {
            URL url = new URL(urlString);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(timeout);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);

            httpURLConnection.connect();

            new InputStreamReader(httpURLConnection.getInputStream());

            if (httpURLConnection.getResponseCode() != 200) {
                httpURLConnection.disconnect();
                httpURLConnection = null;
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return httpURLConnection;
    }

    public static boolean downloadFile(String downloadUrl, String destPath, String referer) {
        return downloadFile(downloadUrl, destPath, "type/text", referer);
    }

    public static boolean downloadFile(String downloadUrl, String destPath, String contentType, String referer) {

        InputStream in = null;
        RandomAccessFile out = null;
        HttpURLConnection httpURLConnection = null;
        boolean ok = false;
        String tmpFile = destPath + ".tmp";

        try {

            URL url = new URL(downloadUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");

            if (contentType != null)
                httpURLConnection.setRequestProperty("Content-Type", contentType);

            if (referer != null) {
                httpURLConnection.setRequestProperty("Referer", referer);
            }

            httpURLConnection.setDoOutput(false);
            httpURLConnection.setDoInput(true);
            httpURLConnection.connect();

            if (httpURLConnection.getResponseCode() == 200) {

                FileUtil.makeSureFolderOfFileExist(tmpFile);
                in = new BufferedInputStream(httpURLConnection.getInputStream());
                out = new RandomAccessFile(tmpFile,"rw");

                int bytesRead = 0;
                byte b[] = new byte[2048];
                while (bytesRead >= 0) {
                    bytesRead = in.read(b);
                    if (bytesRead >= 0) {
                        out.write(b,0,bytesRead);
                    }
                }

            }

            httpURLConnection.disconnect();
            httpURLConnection = null;
            ok = true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {

            Utils.closeSilently(in);
            Utils.closeSilently(out);

            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }

            File f = new File(tmpFile);
            if (ok) {
                ok = f.renameTo(new File(destPath));
            }

            if (!ok) {
                if (f.exists()) {
                    f.delete();
                }
            }
        }

        return true;
    }
}
