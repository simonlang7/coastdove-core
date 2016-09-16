package simonlang.coastdove.core.ui.detectable_app_details;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import simonlang.coastdove.core.R;
import simonlang.coastdove.core.ipc.Module;
import simonlang.coastdove.core.ui.LoadableListFragment;

/**
 * ListFragment displayed in DetectableAppDetailsActivity, shows a list
 * of modules associated with the app
 */
public class ModuleListFragment extends LoadableListFragment<Module> {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.loaderID = 300;
    }

    @Override
    public Loader<ArrayList<Module>> onCreateLoader(int id, Bundle args) {
        String appPackageName = ((DetectableAppDetailsActivity)getActivity()).getAppPackageName();
        return new SQLiteTableLoader(getActivity(), appPackageName);
    }

    @Override
    protected void setUpListAdapter() {
        String appPackageName = ((DetectableAppDetailsActivity)getActivity()).getAppPackageName();
        this.adapter = new ModuleListAdapter(getActivity(), appPackageName);
        setListAdapter(this.adapter);
    }

    @Override
    protected void addProgressBarToViewGroup() {
        ViewGroup root = (ViewGroup)getActivity().findViewById(R.id.fragment_module_list);
        root.addView(this.progressBar);
    }
}
