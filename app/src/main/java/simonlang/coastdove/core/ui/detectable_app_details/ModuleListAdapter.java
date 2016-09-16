/*  DetectAppScreen
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

package simonlang.coastdove.core.ui.detectable_app_details;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import simonlang.coastdove.core.CoastDoveService;
import simonlang.coastdove.core.R;
import simonlang.coastdove.core.ipc.Module;

/**
 * Adapter for modules
 */
public class ModuleListAdapter extends ArrayAdapter<Module> {
    private LayoutInflater inflater;
    private String appPackageName;

    public ModuleListAdapter(Context context, String appPackageName) {
        super(context, R.layout.list_item_module);
        this.inflater = LayoutInflater.from(getContext());
        this.appPackageName = appPackageName;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = this.inflater.inflate(R.layout.list_item_module, parent, false);

            holder.moduleName = (TextView)convertView.findViewById(R.id.module_name);
            holder.activateSwitch = (Switch)convertView.findViewById(R.id.module_activate_switch);

            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        final Module item = getItem(position);
        holder.moduleName.setText(item.moduleName);
        boolean moduleActive = CoastDoveService.appEnabledOnListener(getContext(), item.serviceClassName, appPackageName);
        holder.activateSwitch.setOnCheckedChangeListener(null);
        holder.activateSwitch.setChecked(moduleActive);
        holder.activateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    CoastDoveService.addListenerForApp(getContext(), item.servicePackageName, item.serviceClassName, appPackageName);
                else
                    CoastDoveService.removeListenerForApp(getContext(), item.serviceClassName, appPackageName);
            }
        });

        final Switch activateSwitch = holder.activateSwitch;
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateSwitch.setChecked(!activateSwitch.isChecked());
            }
        });

        return convertView;
    }

    private class ViewHolder {
        TextView moduleName;
        Switch activateSwitch;
    }
}
