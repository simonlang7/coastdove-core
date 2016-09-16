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

package simonlang.coastdove.core.ipc;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import simonlang.coastdove.lib.CoastDoveModules;

/**
 * Service to register Coast Dove modules (any external apps to communicate with Coast Dove over IPC)
 */
public class ModuleRegisteringService extends IntentService {

    /**
     * Creates a ModuleRegisteringService
     */
    public ModuleRegisteringService() {
        super("ModuleRegisteringService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String moduleName = intent.getStringExtra(CoastDoveModules.DATA_MODULE_NAME);
        String servicePackageName = intent.getStringExtra(CoastDoveModules.DATA_SERVICE_PACKAGE_NAME);
        String serviceClassName = intent.getStringExtra(CoastDoveModules.DATA_SERVICE_CLASS_NAME);
        ArrayList<String> associatedApps = intent.getStringArrayListExtra(CoastDoveModules.DATA_ASSOCIATED_APPS);

        deleteFromSQLiteDB(getApplicationContext(), serviceClassName);
        writeToSQLiteDB(getApplicationContext(), moduleName, servicePackageName, serviceClassName, associatedApps);
        Log.d("ModuleRegService", "Registered");
    }

    /**
     * Writes module information to the according SQLite database
     */
    public static void writeToSQLiteDB(Context context, String moduleName, String servicePackageName, String serviceClassName, ArrayList<String> associatedApps) {
        ModulesDbHelper dbHelper = new ModulesDbHelper(context.getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransactionNonExclusive();

        ContentValues values = new ContentValues();
        values.put(ModulesContract.ModuleTable.COLUMN_NAME_MODULE_NAME, moduleName);
        values.put(ModulesContract.ModuleTable.COLUMN_NAME_SERVICE_PACKAGE_NAME, servicePackageName);
        values.put(ModulesContract.ModuleTable.COLUMN_NAME_SERVICE_CLASS_NAME, serviceClassName);
        long rowId = db.insert(ModulesContract.ModuleTable.TABLE_NAME, null, values);

        for (String app : associatedApps) {
            ContentValues appValues = new ContentValues();
            appValues.put(ModulesContract.AssociatedAppTable.COLUMN_NAME_APP, app);
            appValues.put(ModulesContract.AssociatedAppTable.COLUMN_NAME_MODULE_ID, rowId);
            db.insert(ModulesContract.AssociatedAppTable.TABLE_NAME, null, appValues);
        }

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    /**
     * Deletes a Coast Dove module from the internal SQLite database
     * @param context             App context
     * @param serviceClassName    Service class name of the module to delete
     */
    public static void deleteFromSQLiteDB(Context context, String serviceClassName) {
        ModulesDbHelper dbHelper = new ModulesDbHelper(context.getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransactionNonExclusive();

        // Query all associated apps
        String[] projection = { ModulesContract.ModuleTable._ID };
        String selection = ModulesContract.ModuleTable.COLUMN_NAME_SERVICE_CLASS_NAME + "=?";
        String[] selectionArgs = { serviceClassName };
        List<Integer> moduleIDs = new LinkedList<>();
        Cursor c = db.query(ModulesContract.ModuleTable.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            moduleIDs.add(c.getInt(0));
            c.moveToNext();
        }
        c.close();
        // Delete the module entry
        db.delete(ModulesContract.ModuleTable.TABLE_NAME, selection, selectionArgs);

        // Delete app entries associated with the module
        for (int moduleID : moduleIDs) {
            String appSelection = ModulesContract.AssociatedAppTable.COLUMN_NAME_MODULE_ID + "=?";
            String[] appSelectionArgs = { moduleID+"" };
            db.delete(ModulesContract.AssociatedAppTable.TABLE_NAME, appSelection, appSelectionArgs);
        }

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    /**
     * Returns a list of all modules in the SQLite database
     */
    public static List<Module> queryModules(Context context) {
        // TODO: move to other class
        List<Module> result = new LinkedList<>();
        ModulesDbHelper dbHelper = new ModulesDbHelper(context.getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = { ModulesContract.ModuleTable._ID,
                                ModulesContract.ModuleTable.COLUMN_NAME_MODULE_NAME,
                                ModulesContract.ModuleTable.COLUMN_NAME_SERVICE_PACKAGE_NAME,
                                ModulesContract.ModuleTable.COLUMN_NAME_SERVICE_CLASS_NAME };
        Cursor c = db.query(ModulesContract.ModuleTable.TABLE_NAME, projection, null, null, null, null, null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            int moduleID = c.getInt(0);
            String moduleName = c.getString(1);
            String servicePackageName = c.getString(2);
            String serviceClassName = c.getString(3);
            List<String> associatedApps = new LinkedList<>();

            String[] appProjection = { ModulesContract.AssociatedAppTable.COLUMN_NAME_APP };
            String appSelection = ModulesContract.AssociatedAppTable.COLUMN_NAME_MODULE_ID + "=?";
            String[] appSelectionArgs = { ""+moduleID };
            Cursor c2 = db.query(ModulesContract.AssociatedAppTable.TABLE_NAME, appProjection, appSelection, appSelectionArgs, null, null, null);
            c2.moveToFirst();
            while (!c2.isAfterLast()) {
                associatedApps.add(c2.getString(0));
                c2.moveToNext();
            }

            result.add(new Module(moduleName, serviceClassName, associatedApps, servicePackageName));

            c.moveToNext();
        }

        db.close();
        return result;
    }
}
