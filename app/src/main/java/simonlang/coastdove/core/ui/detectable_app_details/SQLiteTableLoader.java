package simonlang.coastdove.core.ui.detectable_app_details;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.List;

import simonlang.coastdove.core.ipc.Module;
import simonlang.coastdove.core.ipc.ModuleRegisteringService;

/**
 * Loads a list of entries from an SQLite table
 */
public class SQLiteTableLoader extends AsyncTaskLoader<ArrayList<Module>> {
    private String appPackageName;

    public SQLiteTableLoader(Context context, String appPackageName) {
        super(context);
        this.appPackageName = appPackageName;
    }

    @Override
    public ArrayList<Module> loadInBackground() {
        List<Module> modules = ModuleRegisteringService.queryModules(getContext());
        ArrayList<Module> data = new ArrayList<>();
        for (Module module : modules) {
            if (module.associatedApps.contains(appPackageName) || module.associatedApps.contains("*"))
                data.add(module);
        }
        return data;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }
}
