package net.mafro.android.wakeonlan

import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.apache.commons.lang3.StringUtils

// TODO: Rename actions, choose action names that describe tasks that this
// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
const val ACTION_WAKE = "net.mafro.android.wakeonlan.action.FOO"

// TODO: Rename parameters
const val EXTRA_ITEM_ID = "item_id"

private const val EXTRA_INT_DEFAULT_VAL = -1

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * TODO: Customize class - update intent actions and extra parameters.
 */
class WidgetWakeService : IntentService("WidgetWakeService") {
    override fun onHandleIntent(intent: Intent?) {
        intent ?: return
        if(!StringUtils.startsWith(intent.action, ACTION_WAKE)) return
        try {
            val itemId = intent.getIntExtra(EXTRA_ITEM_ID, EXTRA_INT_DEFAULT_VAL)
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, EXTRA_INT_DEFAULT_VAL)
            if (itemId == EXTRA_INT_DEFAULT_VAL || widgetId == EXTRA_INT_DEFAULT_VAL) return
            historyController.createWakePacketCompletable(itemId).blockingAwait()
            //Toasts must be shown on UI thread
            Observable.just("").observeOn(AndroidSchedulers.mainThread()).doOnNext {
                MagicPacketSentAction(this.applicationContext).run()
            }.subscribe()
        } catch (exception : Exception) {
            //Toasts must be shown on UI thread
            Observable.just("").observeOn(AndroidSchedulers.mainThread()).doOnNext {
                MagicPacketErrorAction(this.applicationContext).accept(exception)
            }.subscribe()
        }
    }
}
