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

import android.app.Activity
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import java.util.*


/**
 * Class handles all functions of the history ListView
 */
class HistoryListHandler(private val parent: Activity, private val view: ListView) : OnItemClickListener {
    private var cursor: Cursor? = null
    private val listeners: MutableList<HistoryListClickListener>


    init {
        this.listeners = ArrayList()
    }

    fun bind(sort_mode: Int) {
        var orderBy: String? = null
        when (sort_mode) {
            WakeOnLanActivity.CREATED -> orderBy = History.Items.IS_STARRED + " DESC, " + History.Items.CREATED_DATE + " DESC"
            WakeOnLanActivity.LAST_USED -> orderBy = History.Items.IS_STARRED + " DESC, " + History.Items.LAST_USED_DATE + " DESC"
            WakeOnLanActivity.USED_COUNT -> orderBy = History.Items.IS_STARRED + " DESC, " + History.Items.USED_COUNT + " DESC"
        }

        // determine if we render the favourite star buttons
        var showStars = false
        if (parent is WakeOnLanActivity) {
            showStars = true
        }

        // load History cursor via custom ResourceAdapter
        cursor = parent.contentResolver.query(History.Items.CONTENT_URI, PROJECTION, null, null, orderBy)
        val adapter = HistoryAdapter(parent, showStars)

        // register self as listener for item clicks
        view.onItemClickListener = this

        // bind to the supplied view
        view.adapter = adapter
    }


    override fun onItemClick(av: AdapterView<*>, v: View, position: Int, id: Long) {
        if (position >= 0) {
            // extract item at position of click
            val item = getItem(position)

            // fire onClick event to HistoryListListeners
            for (l in listeners) {
                l.onClick(item)
            }
        }
    }

    fun getItem(position: Int): HistoryItem {
        this.cursor!!.moveToPosition(position)
        return getItem(this.cursor!!)
    }

    fun addToHistory(title: String, mac: String, ip: String, port: Int) {
        var exists = false

        // don't allow duplicates in history list
        if (cursor!!.moveToFirst()) {
            val macColumn = cursor!!.getColumnIndex(History.Items.MAC)
            val ipColumn = cursor!!.getColumnIndex(History.Items.IP)
            val portColumn = cursor!!.getColumnIndex(History.Items.PORT)

            do {
                if (mac == cursor!!.getString(macColumn) && ip == cursor!!.getString(ipColumn) && port == cursor!!.getInt(portColumn)) {
                    exists = true
                    break
                }
            } while (cursor!!.moveToNext())
        }

        // create only if the item doesn't exist
        if (!exists) {
            val values = ContentValues(4)
            values.put(History.Items.TITLE, title)
            values.put(History.Items.MAC, mac)
            values.put(History.Items.IP, ip)
            values.put(History.Items.PORT, port)
            this.parent.contentResolver.insert(History.Items.CONTENT_URI, values)
        }
    }

    fun updateHistory(id: Int, title: String, mac: String, ip: String, port: Int) {
        val values = ContentValues(4)
        values.put(History.Items.TITLE, title)
        values.put(History.Items.MAC, mac)
        values.put(History.Items.IP, ip)
        values.put(History.Items.PORT, port)

        val itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(id))
        this.parent.contentResolver.update(itemUri, values, null, null)
    }

    fun incrementHistory(id: Long) {
        val usedCountColumn = cursor!!.getColumnIndex(History.Items.USED_COUNT)
        val usedCount = cursor!!.getInt(usedCountColumn)

        val values = ContentValues(1)
        values.put(History.Items.USED_COUNT, usedCount + 1)
        values.put(History.Items.LAST_USED_DATE, java.lang.Long.valueOf(System.currentTimeMillis()))

        val itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, java.lang.Long.toString(id))
        this.parent.contentResolver.update(itemUri, values, null, null)
    }

    fun deleteHistory(id: Int) {
        // use HistoryProvider to remove this row
        val itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(id))
        this.parent.contentResolver.delete(itemUri, null, null)
    }

    internal fun addHistoryListClickListener(l: HistoryListClickListener) {
        this.listeners.add(l)
    }

    internal fun removeHistoryListClickListener(l: HistoryListClickListener) {
        this.listeners.remove(l)
    }

    companion object {
        private val PROJECTION = arrayOf(History.Items._ID, History.Items.TITLE, History.Items.MAC, History.Items.IP, History.Items.PORT, History.Items.LAST_USED_DATE, History.Items.USED_COUNT, History.Items.IS_STARRED)

        private fun getItem(cursor: Cursor): HistoryItem {
            val idColumn = cursor.getColumnIndex(History.Items._ID)
            val titleColumn = cursor.getColumnIndex(History.Items.TITLE)
            val macColumn = cursor.getColumnIndex(History.Items.MAC)
            val ipColumn = cursor.getColumnIndex(History.Items.IP)
            val portColumn = cursor.getColumnIndex(History.Items.PORT)

            return HistoryItem(cursor.getInt(idColumn), cursor.getString(titleColumn), cursor.getString(macColumn), cursor.getString(ipColumn), cursor.getInt(portColumn))
        }
    }

}
