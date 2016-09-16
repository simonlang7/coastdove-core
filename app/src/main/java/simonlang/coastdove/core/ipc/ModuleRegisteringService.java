package simonlang.coastdove.core.ipc;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;

import simonlang.coastdove.lib.CoastDoveModules;

/**
 * Service to register Coast Dove modules (any external apps to communicate with Coast Dove over IPC)
 */
public class ModuleRegisteringService extends Service {
    private final class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d("ModRegisteringService", "Received Msg");
            switch (msg.what) {
                case CoastDoveModules.MSG_REGISTER:
                    Bundle data = msg.getData();
                    data.setClassLoader(ModuleRegisteringService.this.getClass().getClassLoader());
                    String servicePackageName = data.getString(CoastDoveModules.DATA_SERVICE_PACKAGE_NAME);
                    String serviceClassName = data.getString(CoastDoveModules.DATA_SERVICE_CLASS_NAME);
                    String[] associatedApps = data.getStringArray(CoastDoveModules.DATA_ASSOCIATED_APPS);
                    ArrayList<String> associatedAppsAL = new ArrayList<>(associatedApps.length);
                    for (String app : associatedApps)
                        associatedAppsAL.add(app);
                    Log.d("Registered", servicePackageName + " / " + serviceClassName + " (" + associatedAppsAL + ")");
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private transient final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("ModRegSrv", "Bound!");
        return mMessenger.getBinder();
    }
}
