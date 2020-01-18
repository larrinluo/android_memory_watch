package com.ttphoto.resource.watch.sdk.performance;

import com.ttphoto.resource.watch.sdk.utils.Utils;

import java.io.BufferedReader;
import java.io.FileReader;

class Stat {
    long user;
    long nice;
    long sys;
    long idle;
    long iowait;
    long irq;
    long softirq;

    long total;
}

class ProcessStat {
    long utime;
    long stime;
    long cutime;
    long cstjume;

    long total;
}

/**
 * CPUInfo - Work on android device befor android 8.0 and root devices
 */
public class CPUInfo {

    private int mPid;
    private float mTotalCPUUsage = 0;
    private float mMyCpuUsage = 0;
    private Stat[] mStats = {new Stat(), new Stat()};
    private ProcessStat[] mProcessStats = {new ProcessStat(), new ProcessStat()};
    int mIdx = -1;

    boolean hasPermssion = true;

    public CPUInfo(int pid) {
        mPid = pid;
    }

    public float getmTotalCpu() {
        return mTotalCPUUsage;
    }

    public float getmMyCpu() {
        return mMyCpuUsage;
    }

    /**
     * 更新/proc/stat、/proc/stat/pid，并基于上次数据计算
     * 两个更新间隔cpu使用率
     */
    public void update(boolean calculate) {

        if (!hasPermssion) // 没有权限读取/proc/stat, 无法统计CPU使用率
            return;

        int nextId = mIdx + 1;
        if (nextId > 1)
            nextId = 0;

        if (readStat(mStats[nextId]) && readProcessStat(mProcessStats[nextId])) {
            if (calculate && mIdx >= 0) { //update mCPUUsage
                long all = mStats[nextId].total - mStats[mIdx].total;
                long processAll = mProcessStats[nextId].total - mProcessStats[mIdx].total;
                long allIdel = mStats[nextId].idle - mStats[mIdx].idle;

                float usage = (float) (all - allIdel) / all * 100f;
                float myUsage = (float) (processAll) / all * 100f;

                mTotalCPUUsage = usage;
                mMyCpuUsage = myUsage;
            }

            mIdx = nextId;
        }
    }

    private boolean readStat(Stat stat) {
        FileReader fileReader = null;
        BufferedReader reader = null;

        try {

            try {
                fileReader = new FileReader("/proc/stat");
                reader = new BufferedReader(fileReader);
            } catch (Exception e) {
                hasPermssion = false;
                mTotalCPUUsage = -1f;
                mMyCpuUsage = -1f;
                return false;
            }

            String line = reader.readLine();

            if (line != null && line.startsWith("cpu")) {
                //cpu  2133 538 2215 17616 342 0 57 0 0 0
                int idx = 3;
                while (idx < line.length() && line.charAt(idx) == ' ') idx++;
                String[] parts = line.substring(idx).split(" ");
                if (parts.length >= 8) {
                    stat.user = Long.parseLong(parts[0].trim());
                    stat.nice = Long.parseLong(parts[1].trim());
                    stat.sys = Long.parseLong(parts[2].trim());
                    stat.idle = Long.parseLong(parts[3].trim());
                    stat.iowait = Long.parseLong(parts[4].trim());
                    stat.irq = Long.parseLong(parts[5].trim());
                    stat.softirq = Long.parseLong(parts[6].trim());
                    stat.total = stat.user + stat.nice + stat.sys + stat.idle + stat.iowait + stat.irq + stat.softirq;

                    return true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.closeSilently(reader);
            Utils.closeSilently(fileReader);
        }

        return false;
    }

    private boolean readProcessStat(ProcessStat stat) {
        try {
            String content = Utils.readProcFile("stat", mPid);
            String[] parts = content.split(" ");
            if (parts.length > 17) {
                stat.utime = Long.parseLong(parts[13].trim());
                stat.stime = Long.parseLong(parts[14].trim());
                stat.cutime = Long.parseLong(parts[15].trim());
                stat.cstjume = Long.parseLong(parts[16].trim());
                stat.total = stat.utime + stat.stime + stat.cutime + stat.cstjume;
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}


