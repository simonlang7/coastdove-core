package simonlang.coastdove.core.ipc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper to access SQLite database for Coast Dove modules
 */
public class ModulesDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "CoastDoveModules.db";

    public ModulesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ModulesContract.SQL_CREATE_MODULES);
        db.execSQL(ModulesContract.SQL_CREATE_ASSOCIATED_APPS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(ModulesContract.SQL_DELETE_MODULES);
        db.execSQL(ModulesContract.SQL_DELETE_ASSOCIATED_APPS);
        onCreate(db);
    }
}
