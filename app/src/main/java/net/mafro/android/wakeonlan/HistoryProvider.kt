/*
Copyright (C) 2008-2014 Matt Black
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
* Neither the name of the author nor the names of its contributors may be used
  to endorse or promote products derived from this software without specific
  prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package net.mafro.android.wakeonlan

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.text.TextUtils
import java.util.*


/**
 * @desc    Custom ContentProvider to wrap underlying datastore
 */
class HistoryProvider : ContentProvider() {

    private var mOpenHelper: DatabaseHelper? = null

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE history ("
                    + History.Items._ID + " INTEGER PRIMARY KEY,"
                    + History.Items.TITLE + " TEXT,"
                    + History.Items.MAC + " TEXT,"
                    + History.Items.IP + " TEXT,"
                    + History.Items.PORT + " INTEGER,"
                    + History.Items.CREATED_DATE + " INTEGER,"
                    + History.Items.LAST_USED_DATE + " INTEGER,"
                    + History.Items.USED_COUNT + " INTEGER DEFAULT 1,"
                    + History.Items.IS_STARRED + " INTEGER DEFAULT 0"
                    + ");")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion == 1 && newVersion == 2) {
                db.execSQL("ALTER TABLE history ADD COLUMN " + History.Items.USED_COUNT + " INTEGER DEFAULT 1;")
                db.execSQL("ALTER TABLE history ADD COLUMN " + History.Items.IS_STARRED + " INTEGER DEFAULT 0;")
            }
        }
    }

    override fun onCreate(): Boolean {
        mOpenHelper = DatabaseHelper(context)
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val qb = SQLiteQueryBuilder()
        qb.tables = HISTORY_TABLE_NAME
        qb.setProjectionMap(sHistoryProjectionMap)

        // if no sort order is specified use the default
        val orderBy: String?
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = History.Items.DEFAULT_SORT_ORDER
        } else {
            orderBy = sortOrder
        }

        // get the database and run the query
        val db = mOpenHelper!!.readableDatabase
        val c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy)

        // tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(context!!.contentResolver, uri)
        return c
    }

    override fun getType(uri: Uri): String? {
        when (sUriMatcher.match(uri)) {
            HISTORY -> return History.Items.CONTENT_TYPE

            HISTORY_ID -> return History.Items.CONTENT_ITEM_TYPE

            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
    }

    override fun insert(uri: Uri, initialValues: ContentValues?): Uri? {
        // validate the requested uri
        if (sUriMatcher.match(uri) != HISTORY) {
            throw IllegalArgumentException("Unknown URI $uri")
        }

        val values: ContentValues
        if (initialValues != null) {
            values = ContentValues(initialValues)
        } else {
            values = ContentValues()
        }

        val now = System.currentTimeMillis()

        // make sure that the fields are all set
        if (!values.containsKey(History.Items.TITLE)) {
            values.put(History.Items.TITLE, "")
        }
        if (!values.containsKey(History.Items.MAC)) {
            values.put(History.Items.MAC, "")
        }
        if (!values.containsKey(History.Items.IP)) {
            values.put(History.Items.IP, "")
        }
        if (!values.containsKey(History.Items.PORT)) {
            values.put(History.Items.PORT, "")
        }
        if (!values.containsKey(History.Items.CREATED_DATE)) {
            values.put(History.Items.CREATED_DATE, now)
        }
        if (!values.containsKey(History.Items.LAST_USED_DATE)) {
            values.put(History.Items.LAST_USED_DATE, now)
        }

        val db = mOpenHelper!!.writableDatabase

        // insert record, 2nd param is NULLABLE field for if values is empty
        val rowId = db.insert(HISTORY_TABLE_NAME, History.Items.MAC, values)
        if (rowId > 0) {
            val histUri = ContentUris.withAppendedId(History.Items.CONTENT_URI, rowId)
            context!!.contentResolver.notifyChange(histUri, null)
            return histUri
        }

        throw SQLException("Failed to insert row into $uri")
    }

    override fun delete(uri: Uri, where: String?, whereArgs: Array<String>?): Int {
        val db = mOpenHelper!!.writableDatabase
        val count: Int
        when (sUriMatcher.match(uri)) {
            HISTORY -> count = db.delete(HISTORY_TABLE_NAME, where, whereArgs)

            HISTORY_ID -> {
                val histId = uri.pathSegments[1]
                count = db.delete(HISTORY_TABLE_NAME, History.Items._ID + "=" + histId
                        + if (!TextUtils.isEmpty(where)) " AND ($where)" else "", whereArgs)
            }

            else -> throw IllegalArgumentException("Unknown URI $uri")
        }

        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    override fun update(uri: Uri, values: ContentValues?, where: String?, whereArgs: Array<String>?): Int {
        val db = mOpenHelper!!.writableDatabase

        val count: Int
        when (sUriMatcher.match(uri)) {
            HISTORY -> count = db.update(HISTORY_TABLE_NAME, values, where, whereArgs)

            HISTORY_ID -> {
                val historyId = uri.pathSegments[1]
                count = db.update(HISTORY_TABLE_NAME, values, History.Items._ID + "=" + historyId + if (!TextUtils.isEmpty(where)) " AND ($where)" else "", whereArgs)
            }

            else -> throw IllegalArgumentException("Unknown URI $uri")
        }

        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    companion object {

        private const val TAG = "HistoryProvider"

        private const val DATABASE_NAME = "wakeonlan_history.db"
        private const val DATABASE_VERSION = 2

        private var sHistoryProjectionMap: HashMap<String, String>? = null

        internal const val HISTORY_TABLE_NAME = "history"

        private const val HISTORY = 1
        private const val HISTORY_ID = 2

        private val sUriMatcher: UriMatcher

        init {
            sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            sUriMatcher.addURI(History.AUTHORITY, "history", HISTORY)
            sUriMatcher.addURI(History.AUTHORITY, "history/#", HISTORY_ID)

            sHistoryProjectionMap = HashMap()
            sHistoryProjectionMap!![History.Items._ID] = History.Items._ID
            sHistoryProjectionMap!![History.Items.TITLE] = History.Items.TITLE
            sHistoryProjectionMap!![History.Items.MAC] = History.Items.MAC
            sHistoryProjectionMap!![History.Items.IP] = History.Items.IP
            sHistoryProjectionMap!![History.Items.PORT] = History.Items.PORT
            sHistoryProjectionMap!![History.Items.CREATED_DATE] = History.Items.CREATED_DATE
            sHistoryProjectionMap!![History.Items.LAST_USED_DATE] = History.Items.LAST_USED_DATE
            sHistoryProjectionMap!![History.Items.USED_COUNT] = History.Items.USED_COUNT
            sHistoryProjectionMap!![History.Items.IS_STARRED] = History.Items.IS_STARRED
        }
    }

}
