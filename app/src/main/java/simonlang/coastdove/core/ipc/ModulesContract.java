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

import android.provider.BaseColumns;

/**
 * Defines everything needed for SQL interaction with the Coast Dove Module DB
 */
public abstract class ModulesContract {
    public static abstract class ModuleTable implements BaseColumns {
        public static final String TABLE_NAME = "module";
        public static final String COLUMN_NAME_MODULE_NAME = "module_name";
        public static final String COLUMN_NAME_SERVICE_PACKAGE_NAME = "service_package_name";
        public static final String COLUMN_NAME_SERVICE_CLASS_NAME = "service_class_name";
    }

    public static abstract class AssociatedAppTable implements BaseColumns {
        public static final String TABLE_NAME = "associated_app";
        public static final String COLUMN_NAME_MODULE_ID = "module_id";
        public static final String COLUMN_NAME_APP = "app";
    }

    public static final String TEXT_TYPE = " TEXT";
    public static final String COMMA_SEP = ", ";

    public static final String SQL_CREATE_MODULES =
            "CREATE TABLE " + ModuleTable.TABLE_NAME + " (" +
                    ModuleTable._ID + " INTEGER PRIMARY KEY," +
                    ModuleTable.COLUMN_NAME_MODULE_NAME + TEXT_TYPE + COMMA_SEP +
                    ModuleTable.COLUMN_NAME_SERVICE_PACKAGE_NAME + TEXT_TYPE + COMMA_SEP +
                    ModuleTable.COLUMN_NAME_SERVICE_CLASS_NAME + TEXT_TYPE
                    + ")";

    public static final String SQL_CREATE_ASSOCIATED_APPS =
            "CREATE TABLE " + AssociatedAppTable.TABLE_NAME + " (" +
                    AssociatedAppTable._ID + " INTEGER PRIMARY KEY," +
                    AssociatedAppTable.COLUMN_NAME_MODULE_ID + " INTEGER" + COMMA_SEP +
                    AssociatedAppTable.COLUMN_NAME_APP + TEXT_TYPE + COMMA_SEP +
                    "FOREIGN KEY (" + AssociatedAppTable.COLUMN_NAME_MODULE_ID + ") REFERENCES " + ModuleTable.TABLE_NAME + "(" + ModuleTable._ID + ")"
                    + " )";

    public static final String SQL_DELETE_MODULES =
            "DROP TABLE IF EXISTS " + ModuleTable.TABLE_NAME;

    public static final String SQL_DELETE_ASSOCIATED_APPS =
            "DROP TABLE IF EXISTS " + AssociatedAppTable.TABLE_NAME;
}
