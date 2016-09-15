package simonlang.coastdove.core;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.Collection;
import java.util.TreeSet;

import simonlang.coastdove.lib.CollatorWrapper;

/**
 * Service connection to Coast Dove listeners
 */
public class ListenerConnection implements ServiceConnection {
    private Messenger mService = null;
    private boolean mBound = false;

    /** Apps this connection listens to, identified by package name */
    private Collection<String> mAssociatedApps;

    public ListenerConnection(String... associatedApps) {
        mAssociatedApps = new TreeSet<>(new CollatorWrapper());
        for (int i = 0; i < associatedApps.length; ++i)
            mAssociatedApps.add(associatedApps[i]);
    }

    public ListenerConnection(Collection<String> associatedApps) {
        mAssociatedApps = new TreeSet<>(new CollatorWrapper());
        mAssociatedApps.addAll(associatedApps);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = new Messenger(service);
        mBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
        mBound = false;
    }

    public void sendMessage(String appPackageName, int type, Bundle data) {
        if (!mBound || !mAssociatedApps.contains(appPackageName))
            return;

        Message msg = Message.obtain(null, type, 0, 0);
        if (data != null)
            msg.setData(data);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e("ListenerConnection", "Cannot send message: " + e.getMessage());
        }
    }
}
