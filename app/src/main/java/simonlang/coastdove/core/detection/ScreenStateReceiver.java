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

package simonlang.coastdove.core.detection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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
