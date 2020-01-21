package com.ttphoto.resource.watch.sdk;

import com.ttphoto.resource.watch.sdk.IAppWatchClient;

public interface IAppWatchCallback {

    void onAppStart(int pid);
    void onUpdate(AppPerformanceInfo info);
    void onAppExist(int pid);

    // ANR预警告
    void onAnr(int pid);

    // 消除处理慢
    void onMessageSlow(int pid, int message, long delay, long dispatch);
}
