/*
Copyright (C) 2013-2014 Yohan Pereira, Matt Black
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

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.RemoteViews

/**
 * @desc    This class is used to setup the home screen widget, as well as handle click events
 */

class WidgetProvider : AppWidgetProvider() {

    /**
     * @desc    this method is called once when the WidgetHost starts (usually when the OS boots).
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val settings = context.getSharedPreferences(WakeOnLanActivity.TAG, 0)

        for (widget_id in appWidgetIds) {
            val item = loadItemPref(settings, widget_id)
                    ?: // item or preferences missing
                    // TODO: delete the widget probably (can't find a way to do this).
                    // maybe set the title of the widget to ERROR
                    continue
            configureWidget(widget_id, item, context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val intentAction = intent.action ?: return

        if (intentAction.startsWith(WIDGET_ONCLICK)) {
            val settings = context.getSharedPreferences(WakeOnLanActivity.TAG, 0)

            // get the widget id
            val widgetId = getWidgetId(intent)
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return
            }

            // get the HistoryItem associated with the widget_id
            val item = loadItemPref(settings, widgetId)

            // send the packet
            MagicPacket.sendPacket(context, item!!.title, item.mac, item.ip, item.port)
        }
    }

    override fun onDeleted(context: Context, id: IntArray) {
        super.onDeleted(context, id)

        val settings = context.getSharedPreferences(WakeOnLanActivity.TAG, 0)

        for (anId in id) {
            deleteItemPref(settings, anId)
        }
    }

    companion object {
        private const val SETTINGS_PREFIX = "widget_"
        const val WIDGET_ONCLICK = "net.mafro.android.wakeonlan.WidgetOnClick"

        /**
         * @desc    gets the widget id from an intent
         */
        fun getWidgetId(intent: Intent): Int {
            val extras = intent.extras
            return extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                    ?: AppWidgetManager.INVALID_APPWIDGET_ID
        }

        /**
         * @desc    configures a widget for the first time. Usually called when creating a widget
         * for the first time or initialising existing widgets when the AppWidgetManager
         * restarts (usually when the phone reboots).
         */
        fun configureWidget(widget_id: Int, item: HistoryItem, context: Context) {
            val views = RemoteViews(context.packageName, R.layout.widget)
            views.setTextViewText(R.id.appwidget_text, item.title)

            // append id to action to prevent clearing the extras bundle
            views.setOnClickPendingIntent(R.id.appwidget_button, getPendingSelfIntent(context, widget_id, WIDGET_ONCLICK + widget_id))

            // tell the widget manager
            val appWidgetManager = AppWidgetManager.getInstance(context)
            appWidgetManager.updateAppWidget(widget_id, views)
        }

        private fun getPendingSelfIntent(context: Context, widget_id: Int, action: String): PendingIntent {
            val intent = Intent(context, WidgetProvider::class.java)
            intent.action = action
            val bundle = Bundle()
            bundle.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widget_id)
            intent.putExtras(bundle)
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        /**
         * @desc    saves the given history item/widget_id combination
         */
        fun saveItemPref(settings: SharedPreferences, item: HistoryItem, widget_id: Int) {
            val editor = settings.edit()

            // store HistoryItem details in settings
            editor.putInt(SETTINGS_PREFIX + widget_id, item.id)
            editor.putString(SETTINGS_PREFIX + widget_id + History.Items.TITLE, item.title)
            editor.putString(SETTINGS_PREFIX + widget_id + History.Items.MAC, item.mac)
            editor.putString(SETTINGS_PREFIX + widget_id + History.Items.IP, item.ip)
            editor.putInt(SETTINGS_PREFIX + widget_id + History.Items.PORT, item.port)
            editor.apply()
        }

        fun deleteItemPref(settings: SharedPreferences, widget_id: Int) {
            val editor = settings.edit()
            editor.remove(SETTINGS_PREFIX + widget_id)
            editor.remove(SETTINGS_PREFIX + widget_id + History.Items.TITLE)
            editor.remove(SETTINGS_PREFIX + widget_id + History.Items.MAC)
            editor.remove(SETTINGS_PREFIX + widget_id + History.Items.IP)
            editor.remove(SETTINGS_PREFIX + widget_id + History.Items.PORT)
            editor.apply()
        }

        /**
         * @desc    load the HistoryItem associated with a widget_id
         */
        fun loadItemPref(settings: SharedPreferences, widget_id: Int): HistoryItem? {
            // get item_id
            val itemId = settings.getInt(SETTINGS_PREFIX + widget_id, -1)

            if (itemId == -1) {
                // No item_id found for given widget return null
                return null
            }

            val title = settings.getString(SETTINGS_PREFIX + widget_id + History.Items.TITLE, "")
            val mac = settings.getString(SETTINGS_PREFIX + widget_id + History.Items.MAC, "")
            val ip = settings.getString(SETTINGS_PREFIX + widget_id + History.Items.IP, "")
            val port = settings.getInt(SETTINGS_PREFIX + widget_id + History.Items.PORT, -1)

            return HistoryItem(itemId, title, mac, ip, port)
        }
    }

}
