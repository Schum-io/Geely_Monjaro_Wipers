package com.ecarx.xui.adaptapi.binder;

public interface IConnectable {
    interface IConnectWatcher {
        void onConnected();
        void onDisConnected();
    }

    void registerConnectWatcher(IConnectWatcher watcher);
    void disconnect();
}
