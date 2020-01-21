// IAppWatchService.aidl
package com.ttphoto.resource.watch.sdk;

// Declare any non-default types here with import statements

interface IAppWatchService {

    void startWatch(long maxJavaHeap, IBinder watchClient);

    // ANR预警告
    void onAnr();

    // 消除处理慢
    void onMessageSlow(int message, long delay, long dispatch);
}
