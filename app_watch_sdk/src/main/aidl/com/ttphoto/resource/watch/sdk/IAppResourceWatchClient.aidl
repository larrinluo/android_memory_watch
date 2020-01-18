// IAppResourceWatchClient.aidl
package com.ttphoto.resource.watch.sdk;

// Declare any non-default types here with import statements

interface IAppResourceWatchClient {

    void dumpJavaHeap(String fileName);  // dump HprofData to file
    void dumpTrace(String traceFile);  // dump Trace to file
}
