package net.mafro.android.wakeonlan

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

internal enum class TabFragments(@StringRes val title: Int, val clazz: Class<out Fragment>) {
    HISTORY(R.string.title_history, HistoryFragment::class.java),
    WAKE(R.string.title_wake, WakeFragment::class.java)
}