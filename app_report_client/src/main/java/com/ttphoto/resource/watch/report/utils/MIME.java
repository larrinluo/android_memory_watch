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
