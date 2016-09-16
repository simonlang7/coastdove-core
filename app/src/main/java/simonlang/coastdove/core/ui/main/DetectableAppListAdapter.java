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

package simonlang.coastdove.core.ui.main;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import simonlang.coastdove.core.R;

/**
 * List adapter for detectable apps
 */
public class DetectableAppListAdapter extends ArrayAdapter<ApplicationInfo> {

    private LayoutInflater inflater;

    public DetectableAppListAdapter(Context context, int resource) {
        super(context, resource);
        this.inflater = LayoutInflater.from(getContext());
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = this.inflater.inflate(R.layout.list_item_detectable_app, parent, false);

            holder.imageView = (ImageView)convertView.findViewById(R.id.detectable_app_icon);
            holder.appName = (TextView)convertView.findViewById(R.id.detectable_app_name);

            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        // Set up image and text
        PackageManager pm = getContext().getPackageManager();
        final ApplicationInfo appInfo = getItem(position);
        holder.imageView.setImageDrawable(appInfo.loadIcon(pm));
        String text = appInfo.loadLabel(pm).toString();
        holder.appName.setText(text);


        return convertView;
    }


    private class ViewHolder {
        ImageView imageView;
        TextView appName;
    }
}
