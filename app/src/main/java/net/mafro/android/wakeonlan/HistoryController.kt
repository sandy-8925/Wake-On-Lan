package net.mafro.android.wakeonlan

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Action
import io.reactivex.schedulers.Schedulers

internal val historyController : HistoryController = HistoryController()

internal class HistoryController {
    private val historyDB : HistoryDatabase = historyDb

    @AnyThread
    internal fun sendWakePacket(historyItemId : Long) {
        Completable.fromRunnable {
            val historyItem = historyDb.historyDao().historyItem(historyItemId)
            MagicPacket.send(historyItem.mac, historyItem.ip, historyItem.port)
            incrementHistory(historyItemId)
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(MagicPacketErrorAction(appContext))
                .doOnComplete(MagicPacketSentAction(appContext))
                .subscribe()
    }

    @WorkerThread
    @Synchronized
    //TODO: Create a mutex per ID and use that to synchronize instead
    private fun incrementHistory(id: Long) {
        val historyItem = historyDB.historyDao().historyItem(id)
        historyItem.usedCount++
        historyItem.lastUsedDate = System.currentTimeMillis()
        historyDB.historyDao().updateItem(historyItem)
    }
}

private class MagicPacketSentAction(val context: Context) : Action {
    override fun run() {
        // display sent message to user
        WakeOnLanActivity.notifyUser(context.getString(R.string.packet_sent), context)
    }
}