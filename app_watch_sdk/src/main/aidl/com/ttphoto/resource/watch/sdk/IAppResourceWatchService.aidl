// IAppResourceWatchService.aidl
package com.ttphoto.resource.watch.sdk;

// Declare any non-default types here with import statements

interface IAppResourceWatchService {

    void startWatch(long maxJavaHeap, IBinder watchClient);

    // ANR预警告
    void onAnrWarnning(int message, long delay, long timeout, String traceFile);

    // 消除处理慢
    void onMessageSlow(int message, long delay, long dispatch);
}
