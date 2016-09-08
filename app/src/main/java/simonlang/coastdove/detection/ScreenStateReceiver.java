package simonlang.coastdove.detection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver for the screen's state (on/off)
 */
public class ScreenStateReceiver extends BroadcastReceiver {

    /** Detectable app for which to record screen state changed */
    private AppDetectionData currentDetectionData;

    /** Creates a new ScreenStateReceiver with no current detection data */
    public ScreenStateReceiver() {
        currentDetectionData = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (currentDetectionData != null) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
                currentDetectionData.onScreenOff();
            else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
                currentDetectionData.onScreenOn();
        }
    }

    public void setCurrentDetectionData(AppDetectionData currentDetectionData) {
        this.currentDetectionData = currentDetectionData;
    }
}