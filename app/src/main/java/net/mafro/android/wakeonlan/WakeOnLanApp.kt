package net.mafro.android.wakeonlan

import android.app.Application
import android.content.Context
import androidx.room.Room
import net.mafro.android.wakeonlan.WakeOnLanActivity.Companion.TAG

class WakeOnLanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        removeOldPrefs()
        historyDb = Room.databaseBuilder(this, HistoryDatabase::class.java, HistoryProvider.DATABASE_NAME)
                .addMigrations(Migration_1_2(), Migration_2_3())
                .build()
        appContext = applicationContext
    }

    private fun removeOldPrefs() {
        // preferences
        val settings = getSharedPreferences(TAG, Context.MODE_PRIVATE)

        // clean up old preferences
        if (settings.contains(CHECK_FOR_UPDATE_PREFS_KEY)) {
            settings.edit().remove(CHECK_FOR_UPDATE_PREFS_KEY)
                    .remove(LAST_UPDATE_PREFS_KEY)
                    .apply()
        }
    }
}

internal lateinit var historyDb : HistoryDatabase
    private set

internal lateinit var appContext: Context
    private set

private const val CHECK_FOR_UPDATE_PREFS_KEY = "check_for_update"
private const val LAST_UPDATE_PREFS_KEY = "last_update"