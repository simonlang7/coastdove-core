package de.uni_bonn.detectappscreen.app_usage.sql;

import android.provider.BaseColumns;

/**
 * Defines everything needed for SQL interaction
 */
public final class CoastContract {
    private CoastContract() {}

    public static abstract class AppTable implements BaseColumns {
        public static final String TABLE_NAME = "app";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_PACKAGE = "package";
    }

    public static abstract class ActivityTable implements BaseColumns {
        public static final String TABLE_NAME = "activity";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_APP_ID = "app_id";
        public static final String COLUMN_NAME_ACTIVITY = "activity";
    }

    public static abstract class DataEntryTable implements BaseColumns {
        public static final String TABLE_NAME = "data_entry";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_ACTIVITY_ID = "activity_id";
        public static final String COLUMN_NAME_COUNT = "count";
        public static final String COLUMN_NAME_TYPE = "type";
    }

    public static abstract class ClickDetailsTable implements BaseColumns {
        public static final String TABLE_NAME = "click_details";
        public static final String COLUMN_NAME_DATA_ENTRY_ID = "data_entry_id";
        public static final String COLUMN_NAME_ANDROID_ID = "android_id";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_CLASS_NAME = "class";
    }

    public static abstract class LayoutDetailsTable implements BaseColumns {
        public static final String TABLE_NAME = "layout_details";
        public static final String COLUMN_NAME_DATA_ENTRY_ID = "data_entry_id";
        public static final String COLUMN_NAME_LAYOUT = "layout";
    }

    public static abstract class ScrollDetailsTable implements BaseColumns {
        public static final String TABLE_NAME = "layout_details";
        public static final String COLUMN_NAME_DATA_ENTRY_ID = "data_entry_id";
        public static final String COLUMN_NAME_ELEMENT = "element";
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_APPS =
            "CREATE TABLE " + AppTable.TABLE_NAME + " (" +
                    AppTable._ID + " INTEGER PRIMARY KEY," +
                    AppTable.COLUMN_NAME_TIMESTAMP + TEXT_TYPE + COMMA_SEP +
                    AppTable.COLUMN_NAME_PACKAGE + TEXT_TYPE
                    + " )";

    private static final String SQL_CREATE_ACTIVITIES =
            "CREATE TABLE " + ActivityTable.TABLE_NAME + " (" +
                    ActivityTable._ID + " INTEGER PRIMARY KEY," +
                    ActivityTable.COLUMN_NAME_TIMESTAMP + TEXT_TYPE + COMMA_SEP +
                    ActivityTable.COLUMN_NAME_APP_ID + " INTEGER FOREIGN KEY REFERENCES " + AppTable.TABLE_NAME + "(" + AppTable._ID + ")"
                    + " )";

    private static final String SQL_CREATE_DATA_ENTRIES =
            "CREATE TABLE " + DataEntryTable.TABLE_NAME + " (" +
                    DataEntryTable._ID + " INTEGER PRIMARY KEY," +
                    DataEntryTable.COLUMN_NAME_TIMESTAMP + TEXT_TYPE + COMMA_SEP +
                    DataEntryTable.COLUMN_NAME_ACTIVITY_ID + " INTEGER FOREIGN KEY REFERENCES " + ActivityTable.TABLE_NAME + "(" + ActivityTable._ID + ")" + COMMA_SEP +
                    DataEntryTable.COLUMN_NAME_COUNT + " INTEGER" + COMMA_SEP +
                    DataEntryTable.COLUMN_NAME_TYPE + TEXT_TYPE
                    + " )";

    private static final String SQL_CREATE_CLICK_DETAILS =
            "CREATE TABLE " + ClickDetailsTable.TABLE_NAME + " (" +
                    ClickDetailsTable._ID + " INTEGER PRIMARY KEY," +
                    ClickDetailsTable.COLUMN_NAME_DATA_ENTRY_ID + " INTEGER FOREIGN KEY REFERENCES " + DataEntryTable.TABLE_NAME + "(" + DataEntryTable._ID + ")" + COMMA_SEP +
                    ClickDetailsTable.COLUMN_NAME_ANDROID_ID + TEXT_TYPE + COMMA_SEP +
                    ClickDetailsTable.COLUMN_NAME_TEXT + TEXT_TYPE + COMMA_SEP +
                    ClickDetailsTable.COLUMN_NAME_CLASS_NAME + TEXT_TYPE
                    + " )";

    private static final String SQL_CREATE_LAYOUT_DETAILS =
            "CREATE TABLE " + LayoutDetailsTable.TABLE_NAME + " (" +
                    LayoutDetailsTable._ID + " INTEGER PRIMARY KEY," +
                    LayoutDetailsTable.COLUMN_NAME_DATA_ENTRY_ID + " INTEGER FOREIGN KEY REFERENCES " + DataEntryTable.TABLE_NAME + "(" + DataEntryTable._ID + ")" + COMMA_SEP +
                    LayoutDetailsTable.COLUMN_NAME_LAYOUT + TEXT_TYPE
                    + " )";

    private static final String SQL_CREATE_SCROLL_DETAILS =
            "CREATE TABLE " + ScrollDetailsTable.TABLE_NAME + " (" +
                    ScrollDetailsTable._ID + " INTEGER PRIMARY KEY," +
                    ScrollDetailsTable.COLUMN_NAME_DATA_ENTRY_ID + " INTEGER FOREIGN KEY REFERENCES " + DataEntryTable.TABLE_NAME + "(" + DataEntryTable._ID + ")" + COMMA_SEP +
                    ScrollDetailsTable.COLUMN_NAME_ELEMENT + TEXT_TYPE
                    + " )";
}
