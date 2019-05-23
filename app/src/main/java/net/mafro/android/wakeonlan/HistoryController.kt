package net.mafro.android.wakeonlan

import android.annotation.SuppressLint
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

    @SuppressLint("CheckResult")
    @AnyThread
    internal fun sendWakePacket(historyItemId : Int) {
        Completable.fromRunnable {
            val historyItem = historyDb.historyDao().historyItem(historyItemId)
            if(historyItem == null) { throw Exception("Could not find item info for item ID = $historyItemId") }
            MagicPacket.send(historyItem.mac, historyItem.ip, historyItem.port)
            incrementHistory(historyItemId)
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MagicPacketSentAction(appContext), MagicPacketErrorAction(appContext))
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
    internal fun deleteHistoryItem(historyItemId: Int) {
        Completable.fromRunnable(DeleteHistItemAction(historyItemId))
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    @AnyThread
    internal fun updateHistoryItem(item : HistoryIt) {
        Completable.fromRunnable(UpdateHistItemAction(item))
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    @AnyThread
    internal fun setIsStarred(id: Int, starredVal: Int) {
        Completable.fromRunnable(UpdateStarredStatusTask(id, starredVal))
                .subscribeOn(Schedulers.io())
                .subscribe()
    }
}

private class UpdateStarredStatusTask(val itemId: Int, val starredVal: Int) : Runnable {
    override fun run() {
        val historyItem = historyDb.historyDao().historyItem(itemId)
        historyItem.starred = starredVal
        historyDb.historyDao().updateItem(historyItem)
    }
}

internal class UpdateHistItemAction(private val item: HistoryIt) : Runnable {
    override fun run() {
        historyDb.historyDao().updateItem(item)
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