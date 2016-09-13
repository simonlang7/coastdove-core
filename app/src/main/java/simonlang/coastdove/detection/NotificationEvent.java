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

package simonlang.coastdove.detection;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import simonlang.coastdove.usage.sql.AppUsageContract;
import simonlang.coastdove.utility.Misc;

/**
 * Event data for a received notification
 */
public class NotificationEvent {
    public static NotificationEvent fromSQLiteDB(SQLiteDatabase db, String appPackageName, int notificationID) {
        NotificationEvent result = new NotificationEvent(appPackageName);
        String[] projection = {
                AppUsageContract.NotificationTable._ID,
                AppUsageContract.NotificationTable.COLUMN_NAME_TIMESTAMP,
                AppUsageContract.NotificationTable.COLUMN_NAME_PACKAGE,
                AppUsageContract.NotificationTable.COLUMN_NAME_CONTENT
        };
        String selection = AppUsageContract.NotificationTable._ID + "=?";
        String[] selectionArgs = { ""+notificationID };

        Cursor c = db.query(AppUsageContract.NotificationTable.TABLE_NAME, projection,
                selection, selectionArgs, null, null, null);
        c.moveToFirst();
        String timestampString = c.getString(1);
        try {
            result.timestamp = new SimpleDateFormat(Misc.DATE_TIME_FORMAT, Locale.US).parse(timestampString);
        } catch (ParseException e) {
            Log.e("NotificationEvent", "Unable to parse timestamp: " + timestampString);
        }
        result.content = c.getString(3);
        c.close();

        return result;
    }

    /** Package name of the app that sent the notification */
    private String appPackageName;
    /** Timestamp of when the notification was received */
    private Date timestamp;
    /** Text content of the notification */
    private String content;

    /**
     * Creates a NotificationEvent using the current time for the timestamp
     * @param appPackageName    Package name of the app that sent the notification
     */
    public NotificationEvent(String appPackageName) {
        this.appPackageName = appPackageName;
        this.content = "";
        this.timestamp = new Date();
    }

    /**
     * Creates a NotificationEvent using the current time for the timestamp
     * @param appPackageName    Package name of the app that sent the notification
     * @param content           Text content of the notification
     */
    public NotificationEvent(String appPackageName, String content) {
        this.appPackageName = appPackageName;
        this.content = content;
        this.timestamp = new Date();
    }

    /**
     * Creates a NotificationEvent
     * @param appPackageName    Package name of the app that sent the notification
     * @param content           Text content of the notification
     * @param timestamp         Timestamp of when the notification was received
     */
    public NotificationEvent(String appPackageName, String content, Date timestamp) {
        this.appPackageName = appPackageName;
        this.content = content;
        this.timestamp = timestamp;
    }

    /**
     * Writes this notification event to the given database
     * @param db    Database to write to, already opened
     */
    public void writeToSQLiteDB(SQLiteDatabase db) {
        String timestampString = new SimpleDateFormat(Misc.DATE_TIME_FORMAT, Locale.US).format(this.timestamp);
        ContentValues values = new ContentValues();
        values.put(AppUsageContract.NotificationTable.COLUMN_NAME_PACKAGE, this.appPackageName);
        values.put(AppUsageContract.NotificationTable.COLUMN_NAME_TIMESTAMP, timestampString);
        values.put(AppUsageContract.NotificationTable.COLUMN_NAME_CONTENT, this.content);

        long rowId = db.insert(AppUsageContract.NotificationTable.TABLE_NAME, null, values);
    }
}
