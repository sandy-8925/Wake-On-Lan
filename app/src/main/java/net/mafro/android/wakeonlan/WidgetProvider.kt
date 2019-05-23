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
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

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

        Observable.fromIterable(appWidgetIds.asIterable())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map { widgetId ->
                    Pair(widgetId, loadItemFromPref(settings, widgetId))
                }.filter {
                    it.second != null
                }.map {
                    @Suppress("UNCHECKED_CAST")
                    it as Pair<Int, HistoryIt>
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { pair ->
                    configureWidget(pair.first, pair.second, context)
                }.subscribe()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val intentAction = intent.action ?: return

        if (intentAction.startsWith(WIDGET_ONCLICK)) {
            // get the widget id
            val widgetId = getWidgetId(intent)
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return
            }

            handleWidgetClick(context, widgetId)
        }
    }

    private fun handleWidgetClick(context: Context, widgetId: Int) {
        val prefs = context.getSharedPreferences(WakeOnLanActivity.TAG, 0)
        // get the HistoryItem associated with the widget_id
        val itemId = getItemIdForWidgetId(prefs, widgetId)
        historyController.sendWakePacket(itemId)
    }

    override fun onDeleted(context: Context, id: IntArray) {
        super.onDeleted(context, id)

        val prefs = context.getSharedPreferences(WakeOnLanActivity.TAG, 0)

        for (anId in id) {
            deleteItemPref(prefs, anId)
        }
    }

    companion object {
        /**
         * @desc    configures a widget for the first time. Usually called when creating a widget
         * for the first time or initialising existing widgets when the AppWidgetManager
         * restarts (usually when the phone reboots).
         */
        internal fun configureWidget(widget_id: Int, item: HistoryIt, context: Context) {
            val views = RemoteViews(context.packageName, R.layout.widget)
            views.setTextViewText(R.id.appwidget_text, item.title)

            // append id to action to prevent clearing the extras bundle
            views.setOnClickPendingIntent(R.id.appwidget_button, getPendingSelfIntent(context, widget_id, WIDGET_ONCLICK + widget_id))

            // tell the widget manager
            val appWidgetManager = AppWidgetManager.getInstance(context)
            appWidgetManager.updateAppWidget(widget_id, views)
        }

        private fun getPendingSelfIntent(context: Context, widget_id: Int, action: String): PendingIntent {
            val bundle = Bundle().apply { putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widget_id) }
            val intent = Intent(context, WidgetProvider::class.java)
                    .setAction(action)
                    .putExtras(bundle)
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        /**
         * @desc    saves the given history item/widget_id combination
         */
        internal fun saveItemPref(settings: SharedPreferences, itemId: Int, widget_id: Int) {
            // store HistoryItem details in settings
            settings.edit()
                    .putInt(SETTINGS_PREFIX + widget_id, itemId)
                    .apply()
        }

    }

    @WorkerThread
    /**
     * @desc    load the HistoryItem associated with a widget_id
     */
    private fun loadItemFromPref(prefs: SharedPreferences, widget_id: Int): HistoryIt? {
        // get item_id
        val itemId = getItemIdForWidgetId(prefs, widget_id)
        return if (itemId == -1) {
            // No item_id found for given widget return null
            null
        } else historyDb.historyDao().historyItem(itemId)

    }

    @AnyThread
    private fun getItemIdForWidgetId(prefs: SharedPreferences, widget_id: Int) =
            prefs.getInt(SETTINGS_PREFIX + widget_id, -1)

    private fun deleteItemPref(prefs: SharedPreferences, widget_id: Int) {
        prefs.edit()
                .remove(SETTINGS_PREFIX + widget_id)
                .remove(SETTINGS_PREFIX + widget_id + History.Items.TITLE)
                .remove(SETTINGS_PREFIX + widget_id + History.Items.MAC)
                .remove(SETTINGS_PREFIX + widget_id + History.Items.IP)
                .remove(SETTINGS_PREFIX + widget_id + History.Items.PORT)
                .apply()
    }
}

private const val SETTINGS_PREFIX = "widget_"
private const val WIDGET_ONCLICK = "net.mafro.android.wakeonlan.WidgetOnClick"

/**
 * @desc    gets the widget id from an intent
 */
internal fun getWidgetId(intent: Intent): Int =
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)