// IAppResourceWatchService.aidl
package com.ttphoto.resource.watch.sdk;

// Declare any non-default types here with import statements

interface IAppResourceWatchService {

    void startWatch(long maxJavaHeap, IBinder watchClient);
}
