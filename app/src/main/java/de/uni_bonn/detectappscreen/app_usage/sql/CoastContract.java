package de.uni_bonn.detectappscreen.app_usage.sql;

import android.provider.BaseColumns;

/**
 * Defines everything needed for SQL interaction
 */
public final class CoastContract {
    private CoastContract() {}

    public static abstract class AppUsageDataTable implements BaseColumns {
        public static final String TABLE_NAME = "app_usage_data";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_PACKAGE = "package";
    }

    public static abstract class ActivityDataTable implements BaseColumns {
        public static final String TABLE_NAME = "activity_data";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_USAGE_DATA_ID = "usage_data_id";
        public static final String COLUMN_NAME_ACTIVITY = "activity";
    }

    public static abstract class DataEntryTable implements BaseColumns {
        public static final String TABLE_NAME = "data_entry";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_ACTIVITY_DATA_ID = "activity_data_id";
        public static final String COLUMN_NAME_COUNT = "count";
        public static final String COLUMN_NAME_TYPE = "type";
    }

    public static abstract class EntryDetailsTable implements BaseColumns {
        public static final String TABLE_NAME = "entry_details";
        public static final String COLUMN_NAME_DATA_ENTRY_ID = "data_entry_id";
//        public static final String COLUMN_NAME_COUNT = "count";
//        public static final String COLUMN_NAME_TYPE = "type";
    }
}
