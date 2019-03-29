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
import android.widget.TabHost.OnTabChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import net.mafro.android.wakeonlan.databinding.ActivityMainBinding


/**
 * @desc    Base activity, handles all UI events except history ListView clicks
 */
class WakeOnLanActivity : AppCompatActivity(), OnTabChangeListener {
    private var th: TabHost? = null
    private var binding: ActivityMainBinding? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding!!.wolaViewpager.adapter = TabsAdapter(supportFragmentManager, this@WakeOnLanActivity)
        binding!!.wolaTablayout.setupWithViewPager(binding!!.wolaViewpager)
        //		doStuff();
    }

    private fun doStuff() {
        // configure tabs
        th = findViewById(R.id.tabhost)

        // tabs only exist in phone layouts
        if (th != null) {
            WakeOnLanActivity.isTablet = true

            val lam = LocalActivityManager(this, false)
            th!!.setup(lam)

            th!!.addTab(th!!.newTabSpec("tab_history").setIndicator(getString(R.string.tab_history), resources.getDrawable(R.drawable.ical)).setContent(R.id.historyview))
            th!!.addTab(th!!.newTabSpec("tab_wake").setIndicator(getString(R.string.tab_wake), resources.getDrawable(R.drawable.wake)).setContent(R.id.wakeview))

            th!!.currentTab = 0

            // register self as tab changed listener
            th!!.setOnTabChangedListener(this)
        } else {
            // set the background colour of the titles
            val historytitle = findViewById<TextView>(R.id.historytitle)
            historytitle.setBackgroundColor(-0x666667)
            val waketitle = findViewById<TextView>(R.id.waketitle)
            waketitle.setBackgroundColor(-0x666667)
        }
    }

    override fun onTabChanged(tabId: String) {
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

    private class TabsAdapter internal constructor(fm: FragmentManager, private val context: Context) : FragmentStatePagerAdapter(fm) {

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

    companion object {
        val TAG = "WakeOnLan"

        val MENU_ITEM_WAKE = Menu.FIRST
        val MENU_ITEM_DELETE = Menu.FIRST + 1
        internal val SORT_MODE_PREFS_KEY = "sort_mode"

        private val _editModeID = 0
        private var typingMode = false

        private var isTablet = false

        private val sort_mode: Int = 0

        val CREATED = 0
        val LAST_USED = 1
        val USED_COUNT = 2

        private val PROJECTION = arrayOf(History.Items._ID, History.Items.TITLE, History.Items.MAC, History.Items.IP, History.Items.PORT, History.Items.LAST_USED_DATE, History.Items.USED_COUNT, History.Items.IS_STARRED)

        private var notification: Toast? = null

        fun notifyUser(message: String, context: Context) {
            if (notification != null) {
                notification!!.setText(message)
                notification!!.show()
            } else {
                notification = Toast.makeText(context, message, Toast.LENGTH_SHORT)
                notification!!.show()
            }
        }
    }
}

