package com.ttphoto.resource.watch.sdk.resources;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Debug;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Memory Info of this process of a given time
 */
public class MemoryInfo {
    public final int mPss;          // total PSS, KB
    public final int mJavaHeap;     // Java heap usage, KB
    public final int mNativeHeap;   // native heap usage, KB
    public final int mGraphics;     // graphic memory usage, KB
    public final int mPrivateOther; // other total, KB
    public final int mStack;        // stack memory uages
    public final int mGfxDev;       // gfx dev
    public final int mEGLMTrack;    // EGL mtrack
    public final int mGLMTrack;     // GL mtrack
    public final int mOtherDev;     // Other dev

    public MemoryInfo() {
        mPss = 0;
        mJavaHeap = 0;
        mNativeHeap = 0;
        mGraphics = 0;
        mPrivateOther = 0;
        mStack = 0;
        mGfxDev = 0;
        mEGLMTrack = 0;
        mGLMTrack = 0;
        mOtherDev = 0;
    }

    public MemoryInfo(int pss,
                      int javaHeap,
                      int nativeHeap,
                      int graphics,
                      int privateOther,
                      int stack,
                      int gfxDev,
                      int eglMtrack,
                      int glMtrack,
                      int otherDev) {
        mPss = pss;
        mJavaHeap = javaHeap;
        mNativeHeap = nativeHeap;
        mGraphics = graphics;
        mPrivateOther = privateOther;
        mStack = stack;
        mGfxDev = gfxDev;
        mEGLMTrack = eglMtrack;
        mGLMTrack = glMtrack;
        mOtherDev = otherDev;
    }

    /**
     * 获取本进程的内存统计信息
     * 1. 通过ActivityManagerService.getProcessMemoryInfo获取本进程的Debug.MemoryInfo
     * 2. 根据系统版本解析Debug.MemoryInfo数据，形成我们自己的MemoryInfo
     *
     * 问题：
     * 1. 为什么用ActivityManagerService.getProcessMemoryInfo获取Debug.MemoryInfo
     *    因为ActivityManagerService运行在systemserver进程中，其中加载了libmtrack库，
     *    可以获取gl等设备相关内存信息，这部分内存没有映射到进程地址空间，不能通过smaps文件
     *    获取。
     *
     *    通过android.os.Debug.getMemoryInfo读取Debug.MemoryInfo, 其中read_memtrack_memory
     *    方法回失败，因为用户态进程在调用memtrack_proc_get总是失败，错误信息是：
     *    Couldn't load memtrack module
     *
     *    实际上ActivityManagerService.getProcessMemoryInfo也是调用android.os.Debug.getMemoryInfo
     *    收集进程的MemoryInfo，其中一部分来自smaps，一部分来自mtrack，由于systemserver
     *    是系统进程，因此能顺利获取mtrack这部分信息。
     *
     * 2. 为什么直接解析Debug.MemoryInfo
     *    主要是出于性能考虑。也可以通过ActivityThread.ApplicationThread.dumpMemInfo方法，将内存信息
     *    dump到文件中，然后解析文件获取内存详细信息。但这样就多了写文件、读文件、解析等一系列性能消耗。
     *
     *    实际上ActivityThread.ApplicationThread.dumpMemInfo也是解析Debug.MemoryInfo形成输出报告。
     *    而Debug.MemoryInfo的结构其实相对还是比较稳定的，
     *      1. android 6.0及之后多路mTrack(graphics部分），一直到android 9没有变化
     *      2. android 6.0之前，没有mTrack，结构一致
     *
     *  3. 私有方法反射问题
     *
     *     target api >= 28后， 私有方法不允许反射了。保持target apu < 28
     */
    public static MemoryInfo dump(Context context, int pid) {
        if (Build.VERSION.SDK_INT < 23)
            return DebugMemoryInfoV5.getMemoryInfo(context, pid);
        else
            return DebugMemoryInfoV6.getMemoryInfo(context, pid);
    }

    /**
     * >= Android 6平台 Debug.MemoryInfo解析方法
     */
    static class DebugMemoryInfoV6 {

        private static int sInit = -1;
        private static Method getSummaryTotalPss = null;
        private static Method getSummaryJavaHeap = null;
        private static Method getSummaryNativeHeap = null;
        private static Method getSummaryGraphics = null;
        private static Method getSummaryStack = null;
        private static Method getOtherPrivate = null;
        private static Method getSummaryPrivateOther = null;

        private static int OTHER_GL_DEV = -1;
        private static int OTHER_GRAPHICS = -1;
        private static int OTHER_GL = -1;
        private static int OTHER_UNKNOWN_DEV = -1;


        public static MemoryInfo getMemoryInfo(Context context, int pid) {
            if (init()) {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                int pids[] = {pid};
                Debug.MemoryInfo[] memoryInfos = am.getProcessMemoryInfo(pids);
                return parse(memoryInfos[0]);
            }

            return new MemoryInfo();
        }

        private static boolean init() {
            if (sInit == -1) {
                try {
                    getSummaryTotalPss = Debug.MemoryInfo.class.getDeclaredMethod("getSummaryTotalPss");
                    getSummaryJavaHeap = Debug.MemoryInfo.class.getDeclaredMethod("getSummaryJavaHeap");
                    getSummaryNativeHeap = Debug.MemoryInfo.class.getDeclaredMethod("getSummaryNativeHeap");
                    getSummaryGraphics = Debug.MemoryInfo.class.getDeclaredMethod("getSummaryGraphics");
                    getSummaryStack = Debug.MemoryInfo.class.getDeclaredMethod("getSummaryStack");
                    getOtherPrivate = Debug.MemoryInfo.class.getDeclaredMethod("getOtherPrivate", int.class);
                    getSummaryPrivateOther = Debug.MemoryInfo.class.getDeclaredMethod("getSummaryPrivateOther");

                    Field fOTHER_GL_DEV = Debug.MemoryInfo.class.getDeclaredField("OTHER_GL_DEV");
                    Field fOTHER_GRAPHICS = Debug.MemoryInfo.class.getDeclaredField("OTHER_GRAPHICS");
                    Field fOTHER_GL = Debug.MemoryInfo.class.getDeclaredField("OTHER_GL");
                    Field fOTHER_UNKNOWN_DEV = Debug.MemoryInfo.class.getDeclaredField("OTHER_UNKNOWN_DEV");

                    OTHER_GL_DEV = fOTHER_GL_DEV.getInt(null);
                    OTHER_GRAPHICS = fOTHER_GRAPHICS.getInt(null);
                    OTHER_GL = fOTHER_GL.getInt(null);
                    OTHER_UNKNOWN_DEV = fOTHER_UNKNOWN_DEV.getInt(null);

                    sInit = 0;
                } catch (Exception e) {
                    sInit = 1;
                }
            }

            return sInit == 0;
        }

        private static MemoryInfo parse(Debug.MemoryInfo debugMemoryInfo) {

            try {
                int pss = (int) getSummaryTotalPss.invoke(debugMemoryInfo);
                int javaHeap = (int) getSummaryJavaHeap.invoke(debugMemoryInfo);
                int nativeHeap = (int) getSummaryNativeHeap.invoke(debugMemoryInfo);
                int graphics = (int) getSummaryGraphics.invoke(debugMemoryInfo);
                int privateOther = (int) getSummaryPrivateOther.invoke(debugMemoryInfo);
                int stack = (int) getSummaryStack.invoke(debugMemoryInfo);
                int gfxDev = (int) getOtherPrivate.invoke(debugMemoryInfo, OTHER_GL_DEV);
                int eglMTrack = (int) getOtherPrivate.invoke(debugMemoryInfo, OTHER_GRAPHICS);
                int glMTrack = (int) getOtherPrivate.invoke(debugMemoryInfo, OTHER_GL);
                int otherDev = (int) getOtherPrivate.invoke(debugMemoryInfo, OTHER_UNKNOWN_DEV);

                return new MemoryInfo(pss, javaHeap, nativeHeap, graphics, privateOther, stack,
                        gfxDev,eglMTrack, glMTrack, otherDev);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return new MemoryInfo();
        }
    }

    /**
     * < android 6.0平台Debug.MemoryInfo解析方法
     */
    static class DebugMemoryInfoV5 {

        private static int sInited = -1;
        private static Field dalvikPss = null;
        private static Field nativePss = null;
        private static Field otherPss = null;
        private static Method getOtherPss = null;

        private static boolean init() {
            if (sInited == -1) {
                try {
                    dalvikPss = Debug.MemoryInfo.class.getDeclaredField("dalvikPss");
                    nativePss = Debug.MemoryInfo.class.getDeclaredField("nativePss");
                    otherPss = Debug.MemoryInfo.class.getDeclaredField("otherPss");

                    getOtherPss = Debug.MemoryInfo.class.getDeclaredMethod("getOtherPss", int.class);

                    sInited = 0;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return sInited == 0;
        }

        public static MemoryInfo getMemoryInfo(Context context, int pid) {
            if (init()) {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                int pids[] = {pid};
                Debug.MemoryInfo[] memoryInfos = am.getProcessMemoryInfo(pids);
                return parse(memoryInfos[0]);
            }

            return new MemoryInfo();
        }

        private static MemoryInfo parse(Debug.MemoryInfo debugMemoryInfo) {
            MemoryInfo memoryInfo = new MemoryInfo();

            try {
                int javaHeap = dalvikPss.getInt(debugMemoryInfo);
                int nativeHeap = nativePss.getInt(debugMemoryInfo);
                int privateOther = otherPss.getInt(debugMemoryInfo);
                int otherDev = (int) getOtherPss.invoke(debugMemoryInfo, 5);
                int pss = memoryInfo.mJavaHeap + memoryInfo.mNativeHeap + memoryInfo.mPrivateOther;
                int graphics = 0;
                int stack = 0;
                int gfxDev = 0;
                int eglMTrack = 0;
                int glMTrack = 0;

                return new MemoryInfo(pss, javaHeap, nativeHeap, graphics, privateOther, stack,
                        gfxDev,eglMTrack, glMTrack, otherDev);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return memoryInfo;
        }
    }
}
