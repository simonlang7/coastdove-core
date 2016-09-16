/*  Coast Dove
    Copyright (C) 2016  Simon Lang
    Contact: simon.lang7 at gmail dot com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package simonlang.coastdove.core.ipc;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Collection;
import java.util.TreeSet;

import simonlang.coastdove.lib.CoastDoveListenerService;
import simonlang.coastdove.lib.CollatorWrapper;

// Note: I've only now started using the mName convention. Might change it in all other files in the future...

/**
 * Service connection to Coast Dove listeners
 */
public class ListenerConnection implements ServiceConnection {
    private final class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    /** Receives messages from the remote service */
    private final Messenger mReplyMessenger = new Messenger(new IncomingHandler());
    /** Sends messages to the remote service */
    private Messenger mService = null;
    /** Whether the service is bound (= we can send messages) or not */
    private boolean mBound = false;

    /** Package of the remote service to connect to */
    private String mServicePackageName;
    /** Full class name (including all packages) of the remote service to connect to */
    private String mServiceFullClassName;
    /** Apps this connection is actually listening to, identified by their package name */
    private Collection<String> mEnabledApps;


    /**
     * Creates a new ListenerConnection
     * @param servicePackageName    Package name of the remote service
     * @param serviceClassName      Full class name (including all packages) of the remote service
     */
    public ListenerConnection(@NonNull String servicePackageName, @NonNull String serviceClassName) {
        mServicePackageName = servicePackageName;
        mServiceFullClassName = serviceClassName;
        mEnabledApps = new TreeSet<>(new CollatorWrapper());
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = new Messenger(service);
        mBound = true;

        Message msg = Message.obtain(null, CoastDoveListenerService.MSG_REPLY_TO, 0, 0);
        msg.replyTo = mReplyMessenger;
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e("ListenerConnection", "Cannot send message with ReplyMessenger: " + e.getMessage());
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBound = false;
        mService = null;
    }

    /**
     * Retrieves an intent needed to bind the remote service
     */
    public Intent getListenerIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(mServicePackageName, mServiceFullClassName));
        return intent;
    }

    /**
     * Sends a message to the remote service
     * @param appPackageName    App to which the contents in data belong
     * @param type              Type of message (see CoastDoveListenerService in CoastDoveLib)
     * @param data              Contents to send
     */
    public void sendMessage(String appPackageName, int type, Bundle data) {
        if (!mBound || !mEnabledApps.contains(appPackageName))
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

    /**
     * Enables the given app, so the connection will listen to it. No effect if already enabled.
     */
    public void enableApp(String appPackageName) {
        mEnabledApps.add(appPackageName);
    }

    /**
     * Disables the given app, so the connection will no longer listen to it. No effect if it wasn't enabled.
     */
    public void disableApp(String appPackageName) {
        mEnabledApps.remove(appPackageName);
    }

    /**
     * Indicates whether the given app is enabled or not
     * @return True if enabled
     */
    public boolean isAppEnabled(String appToCheck) {
        return mEnabledApps.contains(appToCheck);
    }

    /**
     * Indicates whether this service has at least one enabled app (returns true if so)
     */
    public boolean hasEnabledApps() {
        return !mEnabledApps.isEmpty();
    }

    /** Indicates whether the remote service is bound */
    public boolean isBound() {
        return mBound;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ListenerConnection) {
            ListenerConnection other = (ListenerConnection)o;
            return other.mServicePackageName.equals(mServicePackageName)
                    && other.mServiceFullClassName.equals(mServiceFullClassName);
        }
        return false;
    }
}