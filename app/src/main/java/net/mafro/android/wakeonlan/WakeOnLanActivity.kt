/*
Copyright (C) 2008-2014 Matt Black
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

import android.app.LocalActivityManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.EditText
import android.widget.TabHost
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import net.mafro.android.wakeonlan.databinding.ActivityMainBinding


/**
 * @desc    Base activity, handles all UI events except history ListView clicks
 */
class WakeOnLanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.wolaViewpager.adapter = TabsAdapter(supportFragmentManager, this@WakeOnLanActivity)
        binding.wolaTablayout.setupWithViewPager(binding.wolaViewpager)
        //		doStuff();
    }

    private fun doStuff() {
        val th: TabHost? = findViewById(R.id.tabhost)
        // configure tabs

        // tabs only exist in phone layouts
        if (th != null) {
            isTablet = true

            val lam = LocalActivityManager(this, false)
            th.setup(lam)

            th.addTab(th.newTabSpec("tab_history").setIndicator(getString(R.string.tab_history), resources.getDrawable(R.drawable.ical)).setContent(R.id.historyview))
            th.addTab(th.newTabSpec("tab_wake").setIndicator(getString(R.string.tab_wake), resources.getDrawable(R.drawable.wake)).setContent(R.id.wakeview))

            th.currentTab = 0

            // register self as tab changed listener
//            th!!.setOnTabChangedListener(this)
        } else {
            // set the background colour of the titles
            val historytitle = findViewById<TextView>(R.id.historytitle)
            historytitle.setBackgroundColor(-0x666667)
            val waketitle = findViewById<TextView>(R.id.waketitle)
            waketitle.setBackgroundColor(-0x666667)
        }
    }

    private fun onTabChanged(tabId: String) {
        if (tabId == "tab_wake") {
            // enter typing mode - no clear of form until exit typing mode
            typingMode = true

        } else if (tabId == "tab_history") {
            // set form back to defaults, if typing mode has ended (button was clicked)
            if (!typingMode) {
                val vtitle = findViewById<EditText>(R.id.title)
                val vmac = findViewById<EditText>(R.id.mac)
                val vip = findViewById<EditText>(R.id.ip)
                val vport = findViewById<EditText>(R.id.port)

                vtitle.text = null
                vmac.text = null
                vip.setText(MagicPacket.BROADCAST)
                vport.setText(Integer.toString(MagicPacket.PORT))

                // clear any errors
                vmac.error = null

                // reset both our button's text
                val sendWake = findViewById<Button>(R.id.test_wake)
                sendWake.setText(R.string.button_wake)
                val clearWake = findViewById<Button>(R.id.clear_wake)
                clearWake.setText(R.string.button_clear)
            }
        }
    }

    companion object {
        const val TAG = "WakeOnLan"

        const val MENU_ITEM_WAKE = Menu.FIRST
        const val MENU_ITEM_DELETE = Menu.FIRST + 1
        internal const val SORT_MODE_PREFS_KEY = "sort_mode"

        private var typingMode = false

        private var isTablet = false

        const val CREATED = 0
        const val LAST_USED = 1
        const val USED_COUNT = 2

        fun notifyUser(message: String, context: Context) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}

private enum class TabFragments(@StringRes val title: Int, val clazz: Class<out Fragment>) {
    HISTORY(R.string.title_history, HistoryFragment::class.java),
    WAKE(R.string.title_wake, WakeFragment::class.java)
}

private class TabsAdapter constructor(fm: FragmentManager, private val context: Context) : FragmentStatePagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        val tabFragmentEnum = TabFragments.values()[position]
        return Fragment.instantiate(context, tabFragmentEnum.clazz.name)
    }

    override fun getCount(): Int {
        return TabFragments.values().size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TabFragments.values()[position].title)
    }
}