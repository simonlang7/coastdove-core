package simonlang.coastdove.core.ipc;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Contains data for a Coast Dove module
 */
public class Module {
    public final String moduleName;
    public final String servicePackageName;
    public final String serviceClassName;
    public final ArrayList<String> associatedApps;

    public Module(String moduleName, String serviceClassName, Collection<String> associatedApps, String servicePackageName) {
        this.moduleName = moduleName;
        this.servicePackageName = servicePackageName;
        this.serviceClassName = serviceClassName;
        this.associatedApps = new ArrayList<>(associatedApps);
    }

    @Override
    public String toString() {
        return moduleName + " (associated with: " + associatedApps + ")";
    }
}
