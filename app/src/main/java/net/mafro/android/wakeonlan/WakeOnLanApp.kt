package net.mafro.android.wakeonlan

import android.app.Application
import android.content.Context
import net.mafro.android.wakeonlan.WakeOnLanActivity.Companion.TAG

class WakeOnLanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        removeOldPrefs()
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

private const val CHECK_FOR_UPDATE_PREFS_KEY = "check_for_update"
private const val LAST_UPDATE_PREFS_KEY = "last_update"