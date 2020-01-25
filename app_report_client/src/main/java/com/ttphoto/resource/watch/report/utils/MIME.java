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

import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * MIME.
 * @author larrin
 *
 */
public final class MIME {

    private MIME() {
    }

    private static Hashtable<String, String> theMimeTypes = new Hashtable<String, String>();

    static
    {
        StringTokenizer st = new StringTokenizer(
                "css		text/css " +
                        "htm		text/html " +
                        "html		text/html " +
                        "xml		text/xml " +
                        "txt		text/plain " +
                        "asc		text/plain " +
                        "gif		image/gif " +
                        "jpg		image/jpeg " +
                        "jpeg		image/jpeg " +
                        "png		image/png " +
                        "mp3		audio/mpeg " +
                        "m3u		audio/mpeg-url " +
                        "mp4		video/mp4 " +
                        "3gpp		video/3gpp " +
                        "3gp		video/3gpp " +
                        "3gpp2		video/3gpp2 " +
                        "3g2		video/3gpp2 " +
                        "m4v		video/x-m4v " +
                        "m4u		video/vnd.mpegurl " +
                        "avi		video/x-msvideo " +
                        "mpeg		video/mpeg " +
                        "mpg		video/mpeg " +
                        "ogv		video/ogg " +
                        "flv		video/x-flv " +
                        "mov		video/quicktime " +
                        "swf		application/x-shockwave-flash " +
                        "js			application/javascript " +
                        "pdf		application/pdf " +
                        "doc		application/msword " +
                        "ogg		application/x-ogg " +
                        "zip		application/octet-stream " +
                        "exe		application/octet-stream " +
                        "class		application/octet-stream ");
        while (st.hasMoreTokens()) {
            theMimeTypes.put(st.nextToken(), st.nextToken());
        }
    }

    /**
     * 根据类型去mini.
     * @param type 类型
     */
    public static String get(final String type) {
        return theMimeTypes.get(type);
    }

    public static String getFileMime(final String path) {
        return getFileMime(path, "application/octet-stream ");
    }

    public static String getFileMime(final String path, String defaultMime) {
        int pos = path.lastIndexOf(".");
        if (pos != -1) {
            String ext = path.substring(pos + 1).toLowerCase();
            String mime = get(ext);
            if (mime != null)
                return mime;
        }

        return defaultMime;
    }
}
