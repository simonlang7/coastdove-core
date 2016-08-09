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

package de.uni_bonn.detectappscreen.setup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

public class ReverseMap {
    private static final String JSON_TYPE = "ReverseMap";
    private static final int JSON_VERSION_MAJOR = 0;
    private static final int JSON_VERSION_MINOR = 4;

    private JSONObject reverseMap;

    // todo: create java object, then convert to JSON (not right away in the constructor)
    public ReverseMap(List<LayoutIdentificationContainer> layoutDefinitionContainers, Set<String> allIDs) {
        reverseMap = new JSONObject();
        try {
            reverseMap.put("_type", JSON_TYPE);
            // todo: maybe also put version tags
            JSONArray androidIDMap = new JSONArray();
            for (String id : allIDs) {
                JSONObject layoutsAssociatedObject = new JSONObject();
                layoutsAssociatedObject.put("androidID", id);
                JSONArray layoutsAssociatedArray = new JSONArray();
                //TreeSet<String> layoutsAssociated = new TreeSet<>(Collator.getInstance());
                for (LayoutIdentificationContainer container : layoutDefinitionContainers) {
                    if (container.getAndroidIDs().contains(id))
                        layoutsAssociatedArray.put(container.getName());
                }
                layoutsAssociatedObject.put("layouts", layoutsAssociatedArray);
                androidIDMap.put(layoutsAssociatedObject);
            }
            reverseMap.put("androidIDMap", androidIDMap);

        } catch (JSONException e) {
            System.err.println("Error creating ReverseMap: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public JSONObject toJSON() {
        return this.reverseMap;
    }
}