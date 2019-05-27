package net.mafro.android.wakeonlan

import android.app.Application
import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.apache.commons.lang3.StringUtils

const val ACTION_WAKE = "net.mafro.android.wakeonlan.action.FOO"
const val EXTRA_ITEM_ID = "item_id"
private const val EXTRA_INT_DEFAULT_VAL = -1

internal const val WAKE_WIDGET_SERVICE_NOTIF_CHANNELID = "WAKE_WIDGET_SERVICE_NOTIF_CHANNELID"
internal const val WAKE_WIDGET_SERVICE_NOTIF_CHANNEL_NAME = "WAKE_WIDGET_SERVICE_NOTIF_CHANNEL_NAME"

@RequiresApi(Build.VERSION_CODES.O)
internal fun setupWakeServiceNotifChannel(context: Context) {
    val descriptionText = context.getString(R.string.wake_service_notif_title)
    val notifChannel = NotificationChannel(WAKE_WIDGET_SERVICE_NOTIF_CHANNELID, WAKE_WIDGET_SERVICE_NOTIF_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            .apply { description = descriptionText }
    val notificationManager = context.getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(notifChannel)
}

/**
 * An [IntentService] subclass for handling widget click actions
 */
class WidgetWakeService : IntentService("WidgetWakeService") {
    override fun onHandleIntent(intent: Intent?) {

        val notification = NotificationCompat.Builder(this, WAKE_WIDGET_SERVICE_NOTIF_CHANNELID)
                .setContentTitle(getString(R.string.wake_service_notif_title))
                .setContentText(getString(R.string.wake_service_notif_text))
                .setSmallIcon(R.drawable.icon)
                .build()
        startForeground(R.id.widget_wake_service_notif_id, notification)

        intent ?: return
        if(!StringUtils.startsWith(intent.action, ACTION_WAKE)) return

        try {
            val itemId = intent.getIntExtra(EXTRA_ITEM_ID, EXTRA_INT_DEFAULT_VAL)
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, EXTRA_INT_DEFAULT_VAL)
            if (itemId == EXTRA_INT_DEFAULT_VAL || widgetId == EXTRA_INT_DEFAULT_VAL) return
            historyController.createWakePacketCompletable(itemId).blockingAwait()
            //Toasts must be shown on UI thread
            Observable.just("")
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        MagicPacketSentAction(this.applicationContext).run()
                    }.subscribe()
        } catch (exception : Exception) {
            //Toasts must be shown on UI thread
            Observable.just("")
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        MagicPacketErrorAction(this.applicationContext).accept(exception)
                    }.subscribe()
        }
    }
}
