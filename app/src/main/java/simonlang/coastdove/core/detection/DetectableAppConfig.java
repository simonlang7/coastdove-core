package simonlang.coastdove.core.detection;

import android.content.Context;

/**
 * Gathers several options for detectable apps, such as which
 * data to detect and default values
 */

public class DetectableAppConfig {
    public static final boolean DEFAULT_DETECT_LAYOUTS = true;
    public static final boolean DEFAULT_DETECT_INTERACTIONS = true;
    public static final boolean DEFAULT_DETECT_SCREEN_STATE = true;
    public static final boolean DEFAULT_DETECT_NOTIFICATIONS = true;
    public static final boolean DEFAULT_REPLACE_PRIVATE_DATA = false;

    private String appPackageName;
    private Context context;

    private boolean detectLayouts;
    private boolean detectInteractions;
    private boolean detectScreenState;
    private boolean detectNotifications;
    private boolean replacePrivateData;
}
