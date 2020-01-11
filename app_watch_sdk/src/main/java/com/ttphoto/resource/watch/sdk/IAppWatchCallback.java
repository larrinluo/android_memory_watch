package com.ttphoto.resource.watch.sdk;

public interface IAppWatchCallback {

    void onAppStart(int pid);
    void onUpdate(AppResourceInfo info);
    void onAppExist(int pid);
}
