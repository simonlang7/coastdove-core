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
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Collection;
import java.util.TreeSet;

import simonlang.coastdove.core.CoastDoveService;
import simonlang.coastdove.core.detection.AppDetectionData;
import simonlang.coastdove.core.detection.NodeInfoDataExtractor;
import simonlang.coastdove.core.detection.NodeInfoFilter;
import simonlang.coastdove.core.detection.NodeInfoTraverser;
import simonlang.coastdove.core.detection.ViewTreeHelper;
import simonlang.coastdove.core.utility.FileHelper;
import simonlang.coastdove.lib.AppMetaInformation;
import simonlang.coastdove.lib.CoastDoveListenerService;
import simonlang.coastdove.lib.CollatorWrapper;
import simonlang.coastdove.lib.ViewTreeNode;

// Note: I've only now started using the mName convention. Might change it in all other files in the future...

/**
 * Service connection to Coast Dove listeners
 */
public class ListenerConnection implements ServiceConnection {
    private final class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle dataIn = msg.getData();
            dataIn.setClassLoader(ListenerConnection.this.getClass().getClassLoader());

            if ((msg.what & CoastDoveListenerService.REPLY_REQUEST_META_INFORMATION) != 0) {
                String appPackageName = dataIn.getString(CoastDoveListenerService.DATA_APP_PACKAGE_NAME);
                AppDetectionData appDetectionData = CoastDoveService.multiLoader.get(appPackageName);
                if (appDetectionData == null)
                    return;

                AppMetaInformation appMetaInformation = appDetectionData.getAppMetaInformation();
                Bundle dataOut = new Bundle();
                dataOut.putString(CoastDoveListenerService.DATA_APP_PACKAGE_NAME, appPackageName);
                dataOut.putParcelable(CoastDoveListenerService.DATA_META_INFORMATION, appMetaInformation);
                Log.d("ListenerConnection", "Sending AppMetaInformation for " + appPackageName);
                ListenerConnection.this.sendMessage(appPackageName, CoastDoveListenerService.MSG_META_INFORMATION, dataOut);
            }
            if ((msg.what & CoastDoveListenerService.REPLY_REQUEST_VIEW_TREE) != 0) {
                if (CoastDoveService.getService() != null) {
                    sendViewTree(dataIn, true);
                }
            }
            if ((msg.what & CoastDoveListenerService.REPLY_REQUEST_VIEW_TREE_NODE) != 0) {
                if (CoastDoveService.getService() != null) {
                    sendViewTree(dataIn, false);
                }
            }
            if ((msg.what & CoastDoveListenerService.REPLY_REQUEST_ACTION) != 0) {
                if (CoastDoveService.getService() != null && Build.VERSION.SDK_INT >= 21) {
                    performAction(dataIn);
                }
            }
        }

        /**
         * Retrieves the current ViewTree from CoastDoveService and sends it using
         * the ListenerConnection
         * @param dataIn    Bundle that may contain information on the startNodeInfo
         */
        private void sendViewTree(Bundle dataIn, boolean includeSubTree) {
            AccessibilityNodeInfo rootNodeInfo = CoastDoveService.getService().getRootInActiveWindow();

            String appPackageName = rootNodeInfo.getPackageName().toString();
            AppDetectionData appDetectionData = CoastDoveService.multiLoader.get(appPackageName);
            if (appDetectionData == null)
                return;

            AccessibilityNodeInfo startNodeInfo = rootNodeInfo;
            if (dataIn.containsKey(CoastDoveListenerService.DATA_VIEW_TREE_START_NODE_RESOURCE)) {
                final String startNodeResource = dataIn.getString(
                        CoastDoveListenerService.DATA_VIEW_TREE_START_NODE_RESOURCE);

                NodeInfoTraverser<AccessibilityNodeInfo> traverser = new NodeInfoTraverser<>(
                        rootNodeInfo,
                        new NodeInfoDataExtractor<AccessibilityNodeInfo>() {
                            @Override
                            public AccessibilityNodeInfo extractData(AccessibilityNodeInfo nodeInfo) {
                                return nodeInfo;
                            }
                        },
                        new NodeInfoFilter() {
                            @Override
                            public boolean filter(AccessibilityNodeInfo nodeInfo) {
                                return nodeInfo.getViewIdResourceName() != null &&
                                        nodeInfo.getViewIdResourceName().endsWith(startNodeResource);
                            }
                        }
                );
                startNodeInfo = traverser.nextFiltered();
            }

            if (startNodeInfo != null) {
                ViewTreeNode viewTree;
                if (includeSubTree)
                    viewTree = ViewTreeHelper.fromAccessibilityNodeInfo(
                            startNodeInfo, appDetectionData.getReplacementData());
                else
                    viewTree = ViewTreeHelper.flatCopy(startNodeInfo, appDetectionData.getReplacementData());

                Bundle dataOut = new Bundle();
                dataOut.putParcelable(CoastDoveListenerService.DATA_VIEW_TREE, viewTree);
                Log.d("ListenerConnection", "Sending ViewTree");
                ListenerConnection.this.sendMessage(appPackageName,
                        CoastDoveListenerService.MSG_VIEW_TREE, dataOut);
            }
        }

        /**
         * Tries to perform an action on the node specified in dataIn, and sends the result
         * (successful or failed) back to the listener
         */
        private void performAction(Bundle dataIn) {
            int actionID = dataIn.getInt(CoastDoveListenerService.DATA_ACTION);

            AccessibilityNodeInfo nodeInfo = null;
            AccessibilityNodeInfo rootNodeInfo = CoastDoveService.getService().getRootInActiveWindow();
            String appPackageName = rootNodeInfo.getPackageName().toString();
            if (dataIn.containsKey(CoastDoveListenerService.DATA_RESOURCE_ID)) {
                final String androidID = dataIn.getString(CoastDoveListenerService.DATA_RESOURCE_ID);
                NodeInfoTraverser<AccessibilityNodeInfo> traverser = new NodeInfoTraverser<>(rootNodeInfo,
                        new NodeInfoDataExtractor<AccessibilityNodeInfo>() {
                            @Override
                            public AccessibilityNodeInfo extractData(AccessibilityNodeInfo nodeInfo) {
                                return nodeInfo;
                            }
                        },
                        new NodeInfoFilter() {
                            @Override
                            public boolean filter(AccessibilityNodeInfo nodeInfo) {
                                return nodeInfo.getViewIdResourceName() != null &&
                                        nodeInfo.getViewIdResourceName().endsWith(androidID);
                            }
                        });
                nodeInfo = traverser.nextFiltered();
            }
            else if (dataIn.containsKey(CoastDoveListenerService.DATA_VIEW_TREE_NODE)) {
                final ViewTreeNode node = dataIn.getParcelable(CoastDoveListenerService.DATA_VIEW_TREE_NODE);
                NodeInfoTraverser<AccessibilityNodeInfo> traverser = new NodeInfoTraverser<>(rootNodeInfo,
                        new NodeInfoDataExtractor<AccessibilityNodeInfo>() {
                            @Override
                            public AccessibilityNodeInfo extractData(AccessibilityNodeInfo nodeInfo) {
                                return nodeInfo;
                            }
                        },
                        new NodeInfoFilter() {
                            @Override
                            public boolean filter(AccessibilityNodeInfo nodeInfo) {
                                Rect boundsExpected = new Rect();
                                Rect boundsActual = new Rect();
                                node.getBoundsInParent(boundsExpected);
                                nodeInfo.getBoundsInParent(boundsActual);
                                if (!boundsExpected.equals(boundsActual))
                                    return false;
                                node.getBoundsInScreen(boundsExpected);
                                nodeInfo.getBoundsInScreen(boundsActual);
                                return boundsExpected.equals(boundsActual);
                            }
                        });
                nodeInfo = traverser.nextFiltered();
            }
            Bundle dataOut = new Bundle();
            boolean actionPerformed = false;
            dataOut.putInt(CoastDoveListenerService.DATA_ACTION, actionID);
            if (nodeInfo != null) {
                AppDetectionData appDetectionData = CoastDoveService.multiLoader.get(appPackageName);
                if (appDetectionData == null)
                    return;

                actionPerformed = nodeInfo.performAction(actionID);
                ViewTreeNode node = ViewTreeHelper.flatCopy(nodeInfo, appDetectionData.getReplacementData());
                dataOut.putParcelable(CoastDoveListenerService.DATA_VIEW_TREE_NODE, node);
            }
            ListenerConnection.this.sendMessage(appPackageName,
                    CoastDoveListenerService.MSG_ACTION_RESULT, dataOut, actionPerformed ? 1 : 0, 0);
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

        for (String enabledApp : mEnabledApps) {
            Bundle data = new Bundle();
            data.putString(CoastDoveListenerService.DATA_APP_PACKAGE_NAME, enabledApp);
            sendMessage(enabledApp, CoastDoveListenerService.MSG_APP_ENABLED, data);
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
        sendMessage(appPackageName, type, data, 0, 0);
    }

    /**
     * Sends a message to the remote service
     * @param appPackageName    App to which the contents in data belong
     * @param type              Type of message (see CoastDoveListenerService in CoastDoveLib)
     * @param data              Contents to send
     */
    public void sendMessage(String appPackageName, int type, Bundle data, int arg1, int arg2) {
        if (!mBound || !mEnabledApps.contains(appPackageName))
            return;

        Message msg = Message.obtain(null, type, arg1, arg2);
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
     * Also notifies the listener that the app has been enabled.
     */
    public void enableApp(String appPackageName) {
        if (mEnabledApps.contains(appPackageName))
            return;

        mEnabledApps.add(appPackageName);

        if (mBound) {
            Bundle data = new Bundle();
            data.putString(CoastDoveListenerService.DATA_APP_PACKAGE_NAME, appPackageName);
            sendMessage(appPackageName, CoastDoveListenerService.MSG_APP_ENABLED, data);
        }
    }

    /**
     * Disables the given app, so the connection will no longer listen to it. No effect if it wasn't enabled.
     * Also notifies the listener that the app has been disabled.
     */
    public void disableApp(String appPackageName) {
        if (!mEnabledApps.contains(appPackageName))
            return;

        if (mBound) {
            Bundle data = new Bundle();
            data.putString(CoastDoveListenerService.DATA_APP_PACKAGE_NAME, appPackageName);
            sendMessage(appPackageName, CoastDoveListenerService.MSG_APP_DISABLED, data);
        }

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
