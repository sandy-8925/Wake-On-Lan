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
    internal fun sendWakePacket(historyItemId : Int) {
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
    private fun incrementHistory(id: Int) {
        val historyItem = historyDB.historyDao().historyItem(id)
        historyItem.usedCount++
        historyItem.lastUsedDate = Math.max(System.currentTimeMillis(), historyItem.lastUsedDate)
        historyDB.historyDao().updateItem(historyItem)
    }

    @AnyThread
    internal fun deleteHistory(historyItemId: Int) {
        Completable.fromRunnable(DeleteHistItemAction(historyItemId))
                .subscribeOn(Schedulers.io())
                .subscribe()
    }
}

private class MagicPacketSentAction(val context: Context) : Action {
    override fun run() {
        // display sent message to user
        WakeOnLanActivity.notifyUser(context.getString(R.string.packet_sent), context)
    }
}

private class DeleteHistItemAction(private val historyItemId : Int) : Runnable {
    override fun run() {
        val histItem = historyDb.historyDao().historyItem(historyItemId)
        historyDb.historyDao().deleteItem(histItem)
    }
}