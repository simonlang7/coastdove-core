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

package de.uni_bonn.detectappscreen.app_usage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageContract;

/**
 * Data entry containing a detected interaction at a certain point during app usage
 */
public class InteractionDataEntry extends ActivityDataEntry {
    public static ActivityDataEntry fromSQLiteDB(SQLiteDatabase db, Date timestamp, String activity,
                                                 EntryType type, int count, int dataEntryID) {
        String[] projection = {
                AppUsageContract.InteractionDetailsTable.COLUMN_NAME_DATA_ENTRY_ID,
                AppUsageContract.InteractionDetailsTable.COLUMN_NAME_ANDROID_ID,
                AppUsageContract.InteractionDetailsTable.COLUMN_NAME_TEXT,
                AppUsageContract.InteractionDetailsTable.COLUMN_NAME_CLASS_NAME
        };
        String selection = AppUsageContract.InteractionDetailsTable.COLUMN_NAME_DATA_ENTRY_ID + "=?";
        String[] selectionArgs = { ""+dataEntryID };

        Cursor c = db.query(AppUsageContract.InteractionDetailsTable.TABLE_NAME, projection,
                selection, selectionArgs, null, null, null);
        c.moveToFirst();
        Set<InteractionEventData> interaction = new CopyOnWriteArraySet<>();
        while (!c.isAfterLast()) {
            String androidID = c.getString(1);
            String text = c.getString(2);
            String className = c.getString(3);
            InteractionEventData eventData = new InteractionEventData(androidID, text, className);
            interaction.add(eventData);

            c.moveToNext();
        }
        c.close();

        InteractionDataEntry dataEntry = new InteractionDataEntry(timestamp, activity, interaction, type);
        dataEntry.count = count;
        return dataEntry;
    }

    /** Type of the event */
    private ActivityDataEntry.EntryType type;
    /** Elements clicked */
    private Set<InteractionEventData> detectedInteraction;

    public InteractionDataEntry(Date timestamp, String activity, Set<InteractionEventData> detectedInteraction,
                                ActivityDataEntry.EntryType type) {
        super(timestamp, activity);
        this.detectedInteraction = detectedInteraction;
        this.type = type;
    }

    public InteractionDataEntry(JSONObject entryJSON) {
        super(entryJSON);

        this.detectedInteraction = new CopyOnWriteArraySet<>();
        try {
            JSONArray detectedInteractionJSON = entryJSON.getJSONArray("detectedInteraction");
            for (int i = 0; i < detectedInteractionJSON.length(); ++i) {
                JSONObject interactionDataJSON = detectedInteractionJSON.getJSONObject(i);
                InteractionEventData interactionData = new InteractionEventData(interactionDataJSON);
                this.detectedInteraction.add(interactionData);
            }
        } catch (JSONException e) {
            Log.e("InteractionDataEntry", "Unable to read from JSONObject: " + e.getMessage());
        }
    }

    @Override
    public boolean equals(ActivityDataEntry other) {
        if (!super.equals(other))
            return false;

        if (!(other instanceof InteractionDataEntry))
            return false;

        InteractionDataEntry otherEntry = (InteractionDataEntry)other;
        if (this.detectedInteraction.size() != otherEntry.detectedInteraction.size())
            return false;

        outer: for (InteractionEventData clickData : this.detectedInteraction) {
            for (InteractionEventData otherClickData : otherEntry.detectedInteraction) {
                if (clickData.equals(otherClickData))
                    continue outer;
            }
            return false;
        }

        return true;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject result = super.toJSON();
        try {
            JSONArray detectedClickJSON = new JSONArray();
            for (InteractionEventData clickData : detectedInteraction)
                detectedClickJSON.put(clickData.toJSON());
            result.put("detectedInteraction", detectedClickJSON);
        } catch (JSONException e) {
            Log.e("InteractionDataEntry", "Unable to create JSONObject: " + e.getMessage());
        }
        return result;
    }

    @Override
    public long writeToSQLiteDB(SQLiteDatabase db, long activityID) {
        long dataEntryID = super.writeToSQLiteDB(db, activityID);
        for (InteractionEventData data : this.detectedInteraction) {
            ContentValues values = data.toContentValues(dataEntryID);
            long rowId = db.insert(AppUsageContract.InteractionDetailsTable.TABLE_NAME, null, values);
            if (rowId == -1)
                throw new SQLiteException("Unable to add row to " + AppUsageContract.InteractionDetailsTable.TABLE_NAME + ": "
                    + values.toString());
        }
        return dataEntryID;
    }

    @Override
    public String getType() {
        return this.type.name();
    }

    @Override
    public String getTypePretty() {
        return this.type.toString();
    }

    @Override
    public String getContent() {
        return detectedInteraction.toString();
    }

    public Set<InteractionEventData> getDetectedInteraction() {
        return this.detectedInteraction;
    }
}
