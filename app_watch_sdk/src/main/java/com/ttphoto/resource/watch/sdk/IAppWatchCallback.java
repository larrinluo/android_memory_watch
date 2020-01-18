package com.ttphoto.resource.watch.sdk;

import com.ttphoto.resource.watch.sdk.IAppResourceWatchClient;

public interface IAppWatchCallback {

    void onAppStart(int pid);
    void onUpdate(AppResourceInfo info);
    void onAppExist(int pid);

    // ANR预警告
    void onAnrWarnning(IAppResourceWatchClient client, int pid, int message, long delay, long timeout);

    // 消除处理慢
    void onMessageSlow(int pid, int message, long delay, long dispatch);
}
